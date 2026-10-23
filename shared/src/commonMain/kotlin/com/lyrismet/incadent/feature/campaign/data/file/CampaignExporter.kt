package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Combat_event
import com.lyrismet.incadent.db.Combatant
import com.lyrismet.incadent.db.SelectAllWithLocations
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.exchange.withKnownMentionsOnly
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionRefsIn
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import kotlin.io.encoding.Base64
import com.lyrismet.incadent.db.location.SelectAll as PlaceSql
import com.lyrismet.incadent.db.partyMember.SelectAll as PartySql
import com.lyrismet.incadent.db.quest.SelectAll as QuestSql
import com.lyrismet.incadent.db.sessionNote.SelectAll as SessionSql
import com.lyrismet.incadent.db.species.SelectAll as SpeciesSql

private const val BASE64_NUMERATOR = 4
private const val BASE64_DENOMINATOR = 3
private const val UTF8_ONE_LIMIT = 0x80
private const val UTF8_TWO_LIMIT = 0x800
private const val UTF8_TWO = 2
private const val UTF8_THREE = 3
private const val UTF8_FOUR = 4

/** reads one campaign out of the database into the lines of a file */
class CampaignExporter(
    private val database: AppDatabase,
) {
    private val placeQueries = database.locationQueries
    private val npcQueries = database.npcQueries
    private val speciesQueries = database.speciesQueries
    private val questQueries = database.questQueries
    private val partyQueries = database.partyMemberQueries
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
            places = placeQueries.selectAll(campaignId).executeAsList(),
            npcs = npcQueries.selectAllWithLocations(campaignId).executeAsList(),
            quests = questQueries.selectAll(campaignId).executeAsList(),
            sessions = sessionQueries.selectAll(campaignId).executeAsList(),
            species = speciesQueries.selectAll(campaignId).executeAsList(),
            party = partyQueries.selectAll(campaignId).executeAsList(),
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
                ),
            )
            writeEntities(snapshot, selection, scope, ids, writer)
            writeSessions(snapshot, scope, ids, writer)
            writeCombats(snapshot.campaignId, scope, ids, writer)
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
        writeCast(snapshot, scope, ids, writer)
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
                    it.damage,
                    it.notes,
                    it.nicknames,
                    it.hit_points?.toInt(),
                ),
            )
        }
        val depth = scope.places.associateWith(ids::placeDepth)
        snapshot.places
            .filter { it.id in scope.places }
            .sortedBy { depth.getValue(it.id) }
            .forEach {
                writer.write(FileRecord.Place(ids.place(it.id), it.name, it.description, it.parent_id?.let(ids::place)))
            }
    }

    private fun writeCast(
        snapshot: Snapshot,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        snapshot.npcs.filter { it.id in scope.npcs }.forEach { npc ->
            writer.write(
                FileRecord.Npc(
                    id = ids.npc(npc.id),
                    name = npc.name,
                    status = npc.status,
                    description = npc.description,
                    race = npc.race,
                    lifeState = npc.life_state,
                    nicknames = npc.nicknames,
                    speciesId = npc.species_id?.let(ids::species),
                    placeIds =
                        npc.location_ids
                            .toIdList()
                            .filter { it in scope.places }
                            .map(ids::place),
                ),
            )
        }
        snapshot.quests.filter { it.id in scope.quests }.forEach {
            writer.write(
                FileRecord.Quest(
                    id = ids.quest(it.id),
                    title = it.title,
                    status = it.status,
                    reward = it.reward,
                    giverNpcId = it.given_by_npc_id?.takeIf { npc -> npc in scope.npcs }?.let(ids::npc),
                    placeId = it.location_id?.takeIf { place -> place in scope.places }?.let(ids::place),
                    description = it.description,
                ),
            )
        }
        snapshot.party.filter { it.id in scope.party }.forEach {
            writer.write(
                FileRecord.Party(
                    id = ids.party(it.id),
                    name = it.name,
                    characterClass = it.character_class,
                    race = it.race,
                    level = it.level?.toInt(),
                    playerName = it.player_name,
                    isPlayerCharacter = it.is_player_character,
                    presence = it.presence,
                    hpMax = it.hp_max?.toInt(),
                    hpCurrent = it.hp_current?.toInt(),
                    description = it.description,
                    fallen = it.fallen,
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
                FileRecord.Session(ids.session(it.id), it.title, it.session_date, it.ended_at, it.created_at, tags),
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
                    ),
                )
            }
        }
    }

    // the fights saved to a picked session go with it, a link to a record that is not in the file is left empty
    private fun writeCombats(
        campaignId: String,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        val combats =
            database.combatQueries
                .selectAll(campaignId)
                .executeAsList()
                .filter { it.phase == CombatPhase.FINISHED && it.session_note_id in scope.sessions }
                .sortedBy { it.id }
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
            val sessionId = checkNotNull(row.session_note_id)
            writer.write(
                FileRecord.Combat(
                    id = numbers.getValue(row.id),
                    sessionId = ids.session(sessionId),
                    outcome = row.outcome ?: CombatOutcome.INTERRUPTED,
                    startedAt = row.started_at,
                    finishedAt = row.finished_at,
                    rounds = row.round.toInt(),
                    turn = row.turn.toInt(),
                ),
            )
        }
        combats.forEach { row ->
            val members = fighters[row.id].orEmpty().withIndex().associate { (index, it) -> it.id to index + 1L }
            fighters[row.id].orEmpty().forEach {
                writer.write(
                    it.toFighter(numbers.getValue(row.id), members, scope, ids),
                )
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

private fun String.toIdList(): List<Long> = split(',').mapNotNull { it.toLongOrNull() }

// utf-8 size without building the byte array, a surrogate pair is 4 bytes
private fun String.utf8Length(): Int {
    var length = 0
    var index = 0
    while (index < this.length) {
        val char = this[index]
        val paired = char.isHighSurrogate() && index + 1 < this.length && this[index + 1].isLowSurrogate()
        if (paired) index++
        length +=
            when {
                paired -> UTF8_FOUR
                char.code < UTF8_ONE_LIMIT -> 1
                char.code < UTF8_TWO_LIMIT -> UTF8_TWO
                else -> UTF8_THREE
            }
        index++
    }
    return length
}

private fun MentionRef.toExchangeRef(): ExchangeRef = ExchangeRef(type.toExchangeKind(), id)

/** the rows of one campaign as they were read, [catalog] is what the picker offers from them */
@Suppress("LongParameterList")
private class Snapshot(
    val campaignId: String,
    val name: String,
    val places: List<PlaceSql>,
    val npcs: List<SelectAllWithLocations>,
    val quests: List<QuestSql>,
    val sessions: List<SessionSql>,
    val species: List<SpeciesSql>,
    val party: List<PartySql>,
    val entries: Map<Long, List<Session_entry>>,
    val tagNames: Map<Long, List<String>>,
) {
    val catalog: ExchangeCatalog by lazy {
        val rows =
            ExchangeRows(
                sessions = sessions.map { SessionRow(it.id, it.title, it.session_date.date.toString()) },
                places = places.map { PlaceRow(it.id, it.name, it.parent_id) },
                npcs = npcs.map { NpcRow(it.id, it.name, it.species_id, it.location_ids.toIdList()) },
                quests = quests.map { QuestRow(it.id, it.title, it.given_by_npc_id, it.location_id) },
                species = species.map { ExchangeItem(it.id, it.name) },
                party = party.map { ExchangeItem(it.id, it.name) },
                mentions =
                    entries.mapValues { (_, list) ->
                        list.flatMap { mentionRefsIn(it.body) }.map { it.toExchangeRef() }.toSet()
                    },
            )
        val options = rows.options()
        ExchangeCatalog(name, options, rows.graph().withKnownMentionsOnly(options))
    }
}

/** counts what is written and keeps none of it */
private class CountingSink : FileSink {
    var bytes = 0L

    override fun writeLine(line: String) {
        bytes += line.utf8Length() + 1
    }

    override fun close() = Unit
}

/** the file's own number for every exported record, counted from 1 per kind in database order */
internal class FileIds(
    scope: RecordScope,
    private val graph: ExchangeGraph,
) {
    private val species = scope.species.numbered()
    private val places = scope.places.numbered()
    private val npcs = scope.npcs.numbered()
    private val quests = scope.quests.numbered()
    private val party = scope.party.numbered()
    private val sessions = scope.sessions.numbered()

    fun species(id: Long) = species.getValue(id)

    fun place(id: Long) = places.getValue(id)

    fun npc(id: Long) = npcs.getValue(id)

    fun quest(id: Long) = quests.getValue(id)

    fun party(id: Long) = party.getValue(id)

    fun session(id: Long) = sessions.getValue(id)

    fun placeDepth(id: Long): Int =
        generateSequence(graph.placeParent[id]) {
            graph.placeParent[it]
        }.take(places.size).count()

    private val byKind =
        mapOf(
            EntityKind.NPC to npcs,
            EntityKind.PARTY to party,
            EntityKind.LOCATION to places,
            EntityKind.QUEST to quests,
            EntityKind.SPECIES to species,
        )

    /** the file number a note token moves to, null for a record that is not in the file */
    fun mapped(ref: MentionRef): Long? = byKind[ref.type]?.get(ref.id)
}

private fun Set<Long>.numbered(): Map<Long, Long> = sorted().withIndex().associate { (index, id) -> id to index + 1L }
