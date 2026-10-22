package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.exchange.withKnownMentionsOnly
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionRefsIn
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import kotlin.io.encoding.Base64

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
    private val sessionQueries = database.sessionNoteQueries
    private val entryQueries = database.sessionEntryQueries
    private val sessionTagQueries = database.sessionTagQueries

    /** [mentions] reads every note body to find what sessions mention, a plain list of records can do without it */
    fun catalog(
        campaignId: String,
        mentions: Boolean = true,
    ): ExchangeCatalog {
        val places = placeQueries.selectAll(campaignId).executeAsList()
        val byId = places.associateBy { it.id }
        val npcs = npcQueries.selectAllWithLocations(campaignId).executeAsList()
        val quests = questQueries.selectAll(campaignId).executeAsList()
        val sessions = sessionQueries.selectAll(campaignId).executeAsList()
        val bodies =
            if (mentions) {
                entryQueries.selectAll(campaignId).executeAsList().groupBy({ it.session_note_id }, { it.body })
            } else {
                emptyMap()
            }

        fun path(parentId: Long?): String =
            generateSequence(parentId?.let(byId::get)) { it.parent_id?.let(byId::get) }
                .toList()
                .reversed()
                .joinToString(" › ") { it.name }
        val npcPlaces = npcs.associate { npc -> npc.id to npc.location_ids.toIdList() }
        val options =
            ExchangeOptions(
                sessions = sessions.map { ExchangeItem(it.id, it.title, it.session_date.date.toString()) },
                places = places.map { ExchangeItem(it.id, it.name, path(it.parent_id)) },
                npcs =
                    npcs.map { npc ->
                        val where = npcPlaces.getValue(npc.id).mapNotNull(byId::get).joinToString(", ") { it.name }
                        ExchangeItem(npc.id, npc.name, where)
                    },
                quests = quests.map { ExchangeItem(it.id, it.title) },
                species = speciesQueries.selectAll(campaignId).executeAsList().map { ExchangeItem(it.id, it.name) },
                party = partyQueries.selectAll(campaignId).executeAsList().map { ExchangeItem(it.id, it.name) },
            )
        val graph =
            ExchangeGraph(
                placeParent = places.associate { it.id to it.parent_id },
                npcSpecies = npcs.associate { it.id to it.species_id },
                npcPlaces = npcPlaces,
                questGiver = quests.associate { it.id to it.given_by_npc_id },
                questPlace = quests.associate { it.id to it.location_id },
                sessionMentions =
                    sessions.associate { session ->
                        session.id to
                            bodies[session.id]
                                .orEmpty()
                                .flatMap(::mentionRefsIn)
                                .map { it.toExchangeRef() }
                                .toSet()
                    },
            )
        val name =
            database.campaignQueries
                .selectName(campaignId)
                .executeAsOneOrNull()
                .orEmpty()
        return ExchangeCatalog(name, options, graph.withKnownMentionsOnly(options))
    }

    /** a dry run of the file without photos for its text size, and the stored photo sizes for the rest */
    fun estimate(
        campaignId: String,
        selection: RecordSelection,
    ): ExportEstimate {
        val catalog = catalog(campaignId)
        val scope = selection.resolve(catalog.graph)
        val counter = CountingSink()
        write(campaignId, catalog.campaignName, selection.copy(photos = false), counter)
        val sizes =
            placeQueries
                .selectPhotoSizes(campaignId)
                .executeAsList()
                .filter { it.id in scope.places }
                .map { it.size } +
                npcQueries
                    .selectPhotoSizes(campaignId)
                    .executeAsList()
                    .filter { it.id in scope.npcs }
                    .map { it.size } +
                speciesQueries
                    .selectPhotoSizes(
                        campaignId,
                    ).executeAsList()
                    .filter { it.id in scope.species }
                    .map { it.size } +
                partyQueries
                    .selectPhotoSizes(
                        campaignId,
                    ).executeAsList()
                    .filter { it.id in scope.party }
                    .map { it.size }
        val stored = sizes.filter { it > 0 }
        return ExportEstimate(counter.bytes, stored.sum() * BASE64_NUMERATOR / BASE64_DENOMINATOR, stored.size)
    }

    fun write(
        campaignId: String,
        campaignName: String,
        selection: RecordSelection,
        sink: FileSink,
    ) {
        val catalog = catalog(campaignId)
        val scope = selection.resolve(catalog.graph)
        val ids = FileIds(scope, catalog.graph)
        val writer = CampaignFileWriter(sink)
        val records = scope.places.size + scope.npcs.size + scope.quests.size + scope.species.size + scope.party.size
        writer.writeHeader(
            FileHeader(FILE_FORMAT, FILE_VERSION, campaignName, selection.photos, records, scope.sessions.size),
        )
        writeEntities(campaignId, selection, scope, ids, writer)
        writeSessions(campaignId, scope, ids, writer)
        if (selection.photos) writePhotos(scope, ids, writer)
        sink.close()
    }

    private fun writeEntities(
        campaignId: String,
        selection: RecordSelection,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        writeWorld(campaignId, scope, ids, writer)
        writeCast(campaignId, scope, ids, writer)
        if (selection.tags) {
            database.tagQueries.selectAll(campaignId).executeAsList().forEach {
                writer.write(FileRecord.Tag(it.name))
            }
        }
        if (selection.phrases) {
            database.phraseSnippetQueries
                .selectAll(
                    campaignId,
                ).executeAsList()
                .forEach { writer.write(FileRecord.Phrase(it.text)) }
        }
    }

    // species and places come first, parents before their children, so every later record finds what it points at
    private fun writeWorld(
        campaignId: String,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        speciesQueries.selectAll(campaignId).executeAsList().filter { it.id in scope.species }.forEach {
            writer.write(
                FileRecord.Species(ids.species(it.id), it.name, it.creature_type, it.damage, it.notes, it.nicknames),
            )
        }
        placeQueries
            .selectAll(campaignId)
            .executeAsList()
            .filter { it.id in scope.places }
            .sortedBy { ids.placeDepth(it.id) }
            .forEach {
                writer.write(FileRecord.Place(ids.place(it.id), it.name, it.description, it.parent_id?.let(ids::place)))
            }
    }

    private fun writeCast(
        campaignId: String,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        npcQueries.selectAllWithLocations(campaignId).executeAsList().filter { it.id in scope.npcs }.forEach { npc ->
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
        questQueries.selectAll(campaignId).executeAsList().filter { it.id in scope.quests }.forEach {
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
        partyQueries.selectAll(campaignId).executeAsList().filter { it.id in scope.party }.forEach {
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
                ),
            )
        }
    }

    private fun writeSessions(
        campaignId: String,
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        val sessions = sessionQueries.selectAll(campaignId).executeAsList().filter { it.id in scope.sessions }
        val ordered = sessions.sortedBy { it.session_date }
        ordered.forEach {
            val tags = sessionTagQueries.selectTagNamesForSession(it.id).executeAsList()
            writer.write(
                FileRecord.Session(ids.session(it.id), it.title, it.session_date, it.ended_at, it.created_at, tags),
            )
        }
        ordered.forEach { session ->
            entryQueries.selectForSession(session.id).executeAsList().forEach { entry ->
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

    // one photo at a time, so memory never holds more than the biggest jpeg and its base64 text
    private fun writePhotos(
        scope: RecordScope,
        ids: FileIds,
        writer: CampaignFileWriter,
    ) {
        scope.species.forEach { id ->
            speciesQueries.selectById(id).executeAsOneOrNull()?.photo?.let {
                writer.writePhoto(PhotoOwner.SPECIES, ids.species(id), it)
            }
        }
        scope.places.forEach { id ->
            placeQueries
                .selectById(
                    id,
                ).executeAsOneOrNull()
                ?.cover
                ?.let { writer.writePhoto(PhotoOwner.PLACE, ids.place(id), it) }
        }
        scope.npcs.forEach { id ->
            npcQueries
                .selectById(
                    id,
                ).executeAsOneOrNull()
                ?.portrait
                ?.let { writer.writePhoto(PhotoOwner.NPC, ids.npc(id), it) }
        }
        scope.party.forEach { id ->
            partyQueries.selectById(id).executeAsOneOrNull()?.portrait?.let {
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

private fun String.toIdList(): List<Long> = split(',').mapNotNull { it.toLongOrNull() }

private fun MentionRef.toExchangeRef(): ExchangeRef = ExchangeRef(type.toExchangeKind(), id)

/** counts what is written and keeps none of it */
private class CountingSink : FileSink {
    var bytes = 0L

    override fun writeLine(line: String) {
        bytes += line.encodeToByteArray().size + 1
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

    /** the file number a note token moves to, null for a record that is not in the file */
    fun mapped(ref: MentionRef): Long? =
        when (ref.type) {
            EntityKind.NPC -> npcs[ref.id]
            EntityKind.PARTY -> party[ref.id]
            EntityKind.LOCATION -> places[ref.id]
            EntityKind.QUEST -> quests[ref.id]
            EntityKind.SPECIES -> species[ref.id]
        }
}

private fun Set<Long>.numbered(): Map<Long, Long> = sorted().withIndex().associate { (index, id) -> id to index + 1L }
