package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Combat_event
import com.lyrismet.incadent.db.Combatant
import com.lyrismet.incadent.db.SelectAllWithLocations
import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.campaign.domain.model.isIn
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import kotlin.io.encoding.Base64
import com.lyrismet.incadent.db.Combat as CombatRow

private const val BASE64_NUMERATOR = 4
private const val BASE64_DENOMINATOR = 3

/** reads one campaign out of the database into the lines of a file */
class CampaignExporter(
    private val database: AppDatabase,
) {
    private val placeQueries = database.locationQueries
    private val npcQueries = database.npcQueries
    private val speciesQueries = database.speciesQueries
    private val questQueries = database.questQueries
    private val partyQueries = database.partyMemberQueries
    private val itemQueries = database.itemQueries
    private val sessionQueries = database.sessionNoteQueries
    private val entryQueries = database.sessionEntryQueries
    private val sessionTagQueries = database.sessionTagQueries

    /** [mentions] reads every note body to find what sessions mention, a plain list of records can do without it */
    fun catalog(
        campaignId: String,
        mentions: Boolean = true,
    ): ExchangeCatalog = load(campaignId, mentions).catalog

    /** a dry run of the file without photos for its text size, and the stored photo sizes for the rest */
    fun estimate(
        campaignId: String,
        selection: RecordSelection,
    ): ExportEstimate {
        val snapshot = load(campaignId, mentions = true)
        val scope = selection.resolve(snapshot.catalog.graph)
        val counter = CountingSink()
        write(snapshot, selection.copy(photos = false), counter)
        val sizes =
            listOf(
                placeQueries.selectPhotoSizes(campaignId).executeAsList().map { it.id to it.size } to scope.places,
                npcQueries.selectPhotoSizes(campaignId).executeAsList().map { it.id to it.size } to scope.npcs,
                speciesQueries.selectPhotoSizes(campaignId).executeAsList().map { it.id to it.size } to scope.species,
                partyQueries.selectPhotoSizes(campaignId).executeAsList().map { it.id to it.size } to scope.party,
                itemQueries.selectPhotoSizes(campaignId).executeAsList().map { it.id to it.size } to scope.items,
            ).flatMap { (rows, ids) -> rows.filter { it.first in ids }.map { it.second } }
        val stored = sizes.filter { it > 0 }
        return ExportEstimate(counter.bytes, stored.sum() * BASE64_NUMERATOR / BASE64_DENOMINATOR, stored.size)
    }

    fun write(
        campaignId: String,
        selection: RecordSelection,
        sink: FileSink,
    ) = write(load(campaignId, mentions = true), selection, sink)

    // every table is read once and shared by the catalog and the writing
    private fun load(
        campaignId: String,
        mentions: Boolean,
    ): Snapshot =
        Snapshot(
            campaignId = campaignId,
            name =
                database.campaignQueries
                    .selectName(campaignId)
                    .executeAsOneOrNull()
                    .orEmpty(),
            originId =
                database.campaignQueries
                    .selectOriginId(campaignId)
                    .executeAsOneOrNull()
                    .orEmpty(),
            places = placeQueries.selectAll(campaignId).executeAsList(),
            npcs = npcQueries.selectAllWithLocations(campaignId).executeAsList(),
            quests = questQueries.selectAll(campaignId).executeAsList(),
            sessions = sessionQueries.selectAll(campaignId).executeAsList(),
            species = speciesQueries.selectAll(campaignId).executeAsList(),
            party = partyQueries.selectAll(campaignId).executeAsList(),
            items = itemQueries.selectAll(campaignId).executeAsList(),
            entries =
                if (mentions) {
                    entryQueries.selectAll(campaignId).executeAsList().groupBy { it.session_note_id }
                } else {
                    emptyMap()
                },
            tagNames =
                sessionTagQueries
                    .selectAllSessionTagNames(campaignId)
                    .executeAsList()
                    .groupBy({ it.session_note_id }, { it.name }),
            liveFight = readLiveFight(database, campaignId),
        )

    private fun write(
        snapshot: Snapshot,
        selection: RecordSelection,
        sink: FileSink,
    ) {
        val graph = snapshot.catalog.graph
        val scope = selection.resolve(graph)
        val ids = FileIds(scope, graph)
        val writer = CampaignFileWriter(sink)
        try {
            writer.writeHeader(
                FileHeader(
                    FILE_FORMAT,
                    FILE_VERSION,
                    snapshot.name,
                    selection.photos,
                    scope.recordCount,
                    scope.sessions.size,
                    snapshot.originId,
                    selection.mode,
                    selection.mode == ExportMode.SELF || selection.partyNotes,
                ),
            )
            writeEntities(snapshot, selection, scope, ids, writer)
            writeSessions(snapshot, scope, ids, writer)
            writeCombats(snapshot.campaignId, selection, scope, ids, writer)
            if (selection.photos) writePhotos(scope, ids, writer)
        } finally {
            sink.close()
        }
    }

    private fun writeEntities(
        snapshot: Snapshot,
        selection: RecordSelection,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        writeWorld(snapshot, scope, ids, writer)
        writeCast(snapshot, selection, scope, ids, writer)
        if (selection.tags) {
            database.tagQueries.selectAll(snapshot.campaignId).executeAsList().forEach {
                writer.write(FileRecord.Tag(it.name))
            }
        }
        if (selection.phrases) {
            database.phraseSnippetQueries.selectAll(snapshot.campaignId).executeAsList().forEach {
                writer.write(FileRecord.Phrase(it.text))
            }
        }
    }

    // species and places come first, parents before their children, so every later record finds what it points at
    private fun writeWorld(
        snapshot: Snapshot,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        snapshot.species.filter { it.id in scope.species }.forEach {
            writer.write(
                FileRecord.Species(
                    ids.species(it.id),
                    it.name,
                    it.creature_type,
                    it.vulnerabilities,
                    ids.text(it.notes),
                    it.hit_points?.toInt(),
                    it.origin_id,
                    it.custom_type_name,
                ),
            )
        }
        val depth = scope.places.associateWith(ids::placeDepth)
        snapshot.places
            .filter { it.id in scope.places }
            .sortedBy { depth.getValue(it.id) }
            .forEach {
                writer.write(
                    FileRecord.Place(
                        ids.place(it.id),
                        it.name,
                        ids.text(it.description),
                        it.parent_id?.let(ids::place),
                        it.origin_id,
                    ),
                )
            }
    }

    private fun npcRecord(
        npc: SelectAllWithLocations,
        scope: RecordScope,
        ids: FileIds,
    ) = FileRecord.Npc(
        id = ids.npc(npc.id),
        name = npc.name,
        status = npc.status,
        description = ids.text(npc.description),
        race = npc.race,
        lifeState = npc.life_state,
        nicknames = npc.nicknames,
        speciesId = npc.species_id?.let(ids::species),
        vulnerabilities = npc.vulnerabilities,
        placeIds =
            npc.location_ids
                .toIdList()
                .filter { it in scope.places }
                .map(ids::place),
        originId = npc.origin_id,
        characterClass = npc.character_class,
        subclass = npc.subclass,
    )

    private fun writeCast(
        snapshot: Snapshot,
        selection: RecordSelection,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        snapshot.npcs.filter { it.id in scope.npcs }.forEach { writer.write(npcRecord(it, scope, ids)) }
        snapshot.quests.filter { it.id in scope.quests }.forEach {
            writer.write(
                FileRecord.Quest(
                    id = ids.quest(it.id),
                    title = it.title,
                    status = it.status,
                    reward = it.reward,
                    giverNpcId = it.given_by_npc_id?.takeIf { npc -> npc in scope.npcs }?.let(ids::npc),
                    placeId = it.location_id?.takeIf { place -> place in scope.places }?.let(ids::place),
                    description = ids.text(it.description),
                    originId = it.origin_id,
                ),
            )
        }
        val personal = selection.mode == ExportMode.SELF
        snapshot.party.filter { it.id in scope.party }.forEach {
            val vitals = partyQueries.selectVitals(it.id).executeAsOne()
            writer.write(
                FileRecord
                    .Party(
                        id = ids.party(it.id),
                        name = it.name,
                        characterClass = it.character_class,
                        subclass = it.subclass,
                        race = it.race,
                        level = it.level?.toInt(),
                        playerName = it.player_name,
                        isPlayerCharacter = it.is_player_character,
                        presence = it.presence,
                        hpMax = it.hp_max?.toInt(),
                        hpCurrent = it.hp_current?.toInt(),
                        description = ids.text(it.description),
                        fallen = it.fallen,
                        originId = it.origin_id,
                        tempHp = vitals.temp_hp.toInt(),
                        armorClass = vitals.armor_class?.toInt(),
                        armorClassFrom = vitals.armor_class_from,
                        deathSuccesses = vitals.death_saves_success.toInt(),
                        deathFailures = vitals.death_saves_failure.toInt(),
                    ).forMode(selection),
            )
        }
        if (personal) writeSheets(snapshot, scope, ids, writer)
        writeItems(snapshot, scope, ids, writer)
    }

    // the sheet and the counters of a character go only in a personal copy, a friend never sees them
    private fun writeSheets(
        snapshot: Snapshot,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        snapshot.party.filter { it.id in scope.party }.forEach { member ->
            readSheetRecords(database, member.id, ids.party(member.id)).forEach(writer::write)
        }
    }

    // after the records that carry them, an owner that did not come along leaves the item shared
    private fun writeItems(
        snapshot: Snapshot,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        snapshot.items.filter { it.id in scope.items }.forEach { item ->
            val owner = snapshot.owners[item.id]?.takeIf { it.isIn(scope) }
            writer.write(
                FileRecord.Item(
                    id = ids.item(item.id),
                    name = item.name,
                    quantity = item.quantity.toInt(),
                    price = item.price?.toInt(),
                    coin = item.coin,
                    ownerKind = owner?.kind,
                    ownerId = owner?.let { ids.owner(it) },
                    notes = ids.text(item.notes),
                    originId = item.origin_id,
                ),
            )
        }
    }

    private fun writeSessions(
        snapshot: Snapshot,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        val ordered = snapshot.sessions.filter { it.id in scope.sessions }.sortedBy { it.session_date }
        ordered.forEach {
            val tags = snapshot.tagNames[it.id].orEmpty()
            writer.write(
                FileRecord.Session(
                    ids.session(it.id),
                    it.title,
                    it.session_date,
                    it.ended_at,
                    it.created_at,
                    tags,
                    it.origin_id,
                ),
            )
        }
        ordered.forEach { session ->
            snapshot.entries[session.id].orEmpty().forEach { entry ->
                writer.write(
                    FileRecord.Entry(
                        sessionId = ids.session(session.id),
                        createdAt = entry.created_at,
                        body = remapMentionTokens(entry.body) { ids.mapped(it) },
                        edited = entry.edited,
                        mentionCheck = entry.mention_check,
                        originId = entry.origin_id,
                    ),
                )
            }
        }
    }

    // the fights saved to a picked session go with it, a link to a record that is not in the file is left empty
    private fun writeCombats(
        campaignId: String,
        selection: RecordSelection,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        val all = database.combatQueries.selectAll(campaignId).executeAsList()
        val live = all.liveRow()?.id?.takeIf { selection.liveFight }
        val combats =
            all
                .filter {
                    if (it.phase ==
                        CombatPhase.FINISHED
                    ) {
                        it.session_note_id in scope.sessions
                    } else {
                        it.id == live
                    }
                }.sortedBy { it.id }
        if (combats.isEmpty()) return
        val fighters =
            database.combatantQueries
                .selectByCampaign(campaignId)
                .executeAsList()
                .groupBy { it.combat_id }
        val events =
            database.combatEventQueries
                .selectByCampaign(campaignId)
                .executeAsList()
                .groupBy { it.combat_id }
        val observations =
            database.speciesHitObservationQueries
                .selectByCampaign(campaignId)
                .executeAsList()
                .groupBy { it.combat_id }
        val numbers = combats.withIndex().associate { (index, row) -> row.id to index + 1L }
        combats.forEach { row ->
            val session = row.session_note_id?.takeIf { it in scope.sessions }?.let(ids::session)
            writer.write(row.toFileRecord(numbers.getValue(row.id), session))
        }
        combats.forEach { row ->
            val members = fighters[row.id].orEmpty().withIndex().associate { (index, it) -> it.id to index + 1L }
            fighters[row.id].orEmpty().forEach {
                val fighter = it.toFighter(numbers.getValue(row.id), members, scope, ids)
                // a fight under way goes whole to a friend, the hit points are what it is made of
                writer.write(if (row.phase == CombatPhase.FINISHED) fighter.forMode(selection) else fighter)
            }
            events[row.id].orEmpty().forEach { event ->
                members[event.target_id]?.let { writer.write(event.toBlow(numbers.getValue(row.id), it, members)) }
            }
            observations[row.id].orEmpty().filter { it.species_id in scope.species }.forEach {
                writer.write(
                    FileRecord.Observation(
                        numbers.getValue(row.id),
                        ids.species(it.species_id),
                        it.damage.toInt(),
                        it.died != 0L,
                    ),
                )
            }
        }
    }

    // one photo at a time, so memory never holds more than the biggest jpeg and its base64 text
    private fun writePhotos(
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        scope.species.forEach { id ->
            speciesQueries.selectPhoto(id).executeAsOneOrNull()?.photo?.let {
                writer.writePhoto(PhotoOwner.SPECIES, ids.species(id), it)
            }
        }
        scope.places.forEach { id ->
            placeQueries.selectPhoto(id).executeAsOneOrNull()?.cover?.let {
                writer.writePhoto(PhotoOwner.PLACE, ids.place(id), it)
            }
        }
        scope.npcs.forEach { id ->
            npcQueries.selectPhoto(id).executeAsOneOrNull()?.portrait?.let {
                writer.writePhoto(PhotoOwner.NPC, ids.npc(id), it)
            }
        }
        scope.party.forEach { id ->
            partyQueries.selectPhoto(id).executeAsOneOrNull()?.portrait?.let {
                writer.writePhoto(PhotoOwner.PARTY, ids.party(id), it)
            }
        }
        scope.items.forEach { id ->
            itemQueries.selectPhoto(id).executeAsOneOrNull()?.photo?.let {
                writer.writePhoto(PhotoOwner.ITEM, ids.item(id), it)
            }
        }
    }

    private fun CampaignFileWriter.writePhoto(
        owner: PhotoOwner,
        id: Long,
        bytes: ByteArray,
    ) = write(FileRecord.Photo(owner, id, Base64.encode(bytes)))
}

private fun Combatant.toFighter(
    combat: Long,
    members: Map<Long, Long>,
    scope: RecordScope,
    ids: FileIds,
) = FileRecord.Fighter(
    combatId = combat,
    id = members.getValue(id),
    name = name,
    side = side,
    npcId = npc_id?.takeIf { it in scope.npcs }?.let(ids::npc),
    speciesId = species_id?.takeIf { it in scope.species }?.let(ids::species),
    partyId = party_member_id?.takeIf { it in scope.party }?.let(ids::party),
    enteredWhole = entered_whole != 0L,
    state = state,
    joinedRound = joined_round.toInt(),
    deathSuccesses = death_successes.toInt(),
    deathFailures = death_failures.toInt(),
    tempHp = temp_hp.toInt(),
    stance = stance,
    captured = captured != 0L,
    dead = dead != 0L,
    conditions = conditions.toList(),
    note = note,
    hpCurrent = hp_current?.toInt(),
    hpMax = hp_max?.toInt(),
    turnOrder = turn_order?.toInt(),
    reactionAvailable = reaction_available != 0L,
    role = role,
    isLabel = is_label != 0L,
)

private fun Combat_event.toBlow(
    combat: Long,
    target: Long,
    members: Map<Long, Long>,
) = FileRecord.Blow(
    combatId = combat,
    round = round.toInt(),
    kind = kind,
    sourceId = source_id?.let(members::get),
    targetId = target,
    amount = amount.toInt(),
    damageType = damage_type,
    effect = effect,
    state = state,
    detail = detail,
    turn = turn.toInt(),
    hit = hit.toInt(),
    raw = raw.toInt(),
    scale = scale,
)

private fun CombatRow.toFileRecord(
    number: Long,
    sessionNumber: Long?,
) = FileRecord.Combat(
    id = number,
    sessionId = sessionNumber,
    phase = phase,
    outcome = outcome ?: CombatOutcome.INTERRUPTED,
    startedAt = started_at,
    finishedAt = finished_at,
    rounds = round.toInt(),
    turn = turn.toInt(),
    originId = origin_id,
)
