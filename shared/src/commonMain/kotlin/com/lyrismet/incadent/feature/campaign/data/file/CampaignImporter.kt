package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.jpegDimensions
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.exchange.nameKey
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// a phone photo is cut to a few hundred pixels on save, a file that claims more than this is not one of ours
private const val MAX_PHOTO_SIDE_PX = 4000
private const val DEFAULT_CAMPAIGN_NAME = "Incadent"
private const val MAX_NAME_CHARS = 80
private const val COPY_SUFFIX = " (2)"

/** writes the records of a file into the database, every id of the file is replaced and every note token follows */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
internal class CampaignImporter(
    private val database: AppDatabase,
    private val makeThumb: (ByteArray, PhotoSlot) -> ByteArray,
) {
    /** the whole import is one transaction, a bad record anywhere leaves the database as it was */
    fun import(
        origin: FileOrigin,
        request: ImportRequest,
    ): ImportResult {
        val source = origin.open()
        try {
            val reader = CampaignFileReader.open(source)
            return database.transactionWithResult { run(reader, request) }
        } finally {
            source.close()
        }
    }

    private fun run(
        reader: CampaignFileReader,
        request: ImportRequest,
    ): ImportResult {
        val campaignId = targetCampaign(request.target, reader.header.name)
        val session = ImportSession(campaignId, request, reader.header.withPhotos)
        while (true) session.accept(reader.nextRecord() ?: break)
        return ImportResult(campaignId, session.records, session.sessions)
    }

    private fun targetCampaign(
        target: ImportTarget,
        fileName: String,
    ): String =
        when (target) {
            is ImportTarget.Existing -> target.campaignId
            is ImportTarget.NewCampaign -> {
                val id = Uuid.random().toString()
                val name =
                    fileName.ifBlank { target.name }.trim().ifBlank { DEFAULT_CAMPAIGN_NAME }.take(
                        MAX_NAME_CHARS,
                    )
                val now = Clock.System.now().toEpochMilliseconds()
                database.campaignQueries.insert(id, name, CampaignStatus.LIVE, now, now)
                id
            }
        }

    private inner class ImportSession(
        private val campaignId: String,
        private val request: ImportRequest,
        private val fileHasPhotos: Boolean,
    ) {
        private val scope = request.scope
        var records = 0
        var sessions = 0

        private val speciesMap = mutableMapOf<Long, Long>()
        private val placeMap = mutableMapOf<Long, Long>()
        private val npcMap = mutableMapOf<Long, Long>()
        private val questMap = mutableMapOf<Long, Long>()
        private val partyMap = mutableMapOf<Long, Long>()
        private val sessionMap = mutableMapOf<Long, Long>()

        // only records written by this import take a photo, a matched one keeps what it has
        private val fresh = mutableSetOf<Pair<PhotoOwner, Long>>()

        private val copies = request.conflicts == ConflictMode.COPY
        private val speciesByName =
            namesOf(
                database.speciesQueries.selectAll(campaignId).executeAsList().map {
                    it.name to
                        it.id
                },
            )
        private val placesByName =
            namesOf(
                database.locationQueries.selectAll(campaignId).executeAsList().map {
                    it.name to
                        it.id
                },
            )
        private val npcsByName =
            namesOf(
                database.npcQueries.selectAll(campaignId).executeAsList().map {
                    it.name to
                        it.id
                },
            )
        private val questsByName =
            namesOf(
                database.questQueries.selectAll(campaignId).executeAsList().map {
                    it.title to
                        it.id
                },
            )
        private val existingParty = database.partyMemberQueries.selectAll(campaignId).executeAsList()
        private val partyByName = namesOf(existingParty.map { it.name to it.id })
        private var playerTaken = existingParty.any { it.is_player_character }
        private var liveTaken =
            database.sessionNoteQueries.selectAll(campaignId).executeAsList().any {
                it.ended_at ==
                    null
            }

        fun accept(record: FileRecord) {
            if (!isWanted(record)) return
            when (record) {
                is FileRecord.Species -> addSpecies(record)
                is FileRecord.Place -> addPlace(record)
                is FileRecord.Npc -> addNpc(record)
                is FileRecord.Quest -> addQuest(record)
                is FileRecord.Party -> addParty(record)
                is FileRecord.Tag -> database.tagQueries.insert(record.name, campaignId)
                is FileRecord.Phrase -> database.phraseSnippetQueries.insert(record.text, campaignId)
                is FileRecord.Session -> addSession(record)
                is FileRecord.Entry -> addEntry(record)
                is FileRecord.Photo -> addPhoto(record)
            }
        }

        // a record outside the pick is skipped, its notes and photo then find no owner and are skipped too
        private fun isWanted(record: FileRecord): Boolean =
            when (record) {
                is FileRecord.Species -> scope.has { species }(record.id)
                is FileRecord.Place -> scope.has { places }(record.id)
                is FileRecord.Npc -> scope.has { npcs }(record.id)
                is FileRecord.Quest -> scope.has { quests }(record.id)
                is FileRecord.Party -> scope.has { party }(record.id)
                is FileRecord.Session -> scope.has { sessions }(record.id)
                is FileRecord.Tag -> request.selection?.tags != false
                is FileRecord.Phrase -> request.selection?.phrases != false
                is FileRecord.Photo -> fileHasPhotos && request.selection?.photos != false
                is FileRecord.Entry -> true
            }

        private fun RecordScope?.has(set: RecordScope.() -> Set<Long>): (Long) -> Boolean =
            { id ->
                this == null ||
                    id in set()
            }

        // a matched record is reused or copied under "(2)", the file's own id then points at the one that was used
        private fun matched(
            byName: Map<String, Long>,
            name: String,
        ): Long? = byName[name.nameKey()]?.takeIf { !copies }

        private fun nameForInsert(
            byName: Map<String, Long>,
            name: String,
        ): String = if (copies && name.nameKey() in byName) name + COPY_SUFFIX else name

        private fun addSpecies(record: FileRecord.Species) {
            speciesMap[record.id] =
                matched(speciesByName, record.name) ?: run {
                    val queries = database.speciesQueries
                    queries.insert(
                        nameForInsert(speciesByName, record.name),
                        record.creatureType,
                        record.damage,
                        record.notes,
                        record.nicknames,
                        null,
                        null,
                        campaignId,
                    )
                    records++
                    fresh += PhotoOwner.SPECIES to record.id
                    queries.lastInsertRowId().executeAsOne()
                }
        }

        private fun addPlace(record: FileRecord.Place) {
            placeMap[record.id] =
                matched(placesByName, record.name) ?: run {
                    val queries = database.locationQueries
                    queries.insert(
                        nameForInsert(placesByName, record.name),
                        record.description,
                        record.parentId?.let(placeMap::get),
                        null,
                        null,
                        campaignId,
                    )
                    records++
                    fresh += PhotoOwner.PLACE to record.id
                    queries.lastInsertRowId().executeAsOne()
                }
        }

        private fun addNpc(record: FileRecord.Npc) {
            npcMap[record.id] =
                matched(npcsByName, record.name) ?: run {
                    val queries = database.npcQueries
                    queries.insert(
                        nameForInsert(npcsByName, record.name),
                        record.status,
                        record.description,
                        record.race,
                        record.lifeState,
                        record.nicknames,
                        record.speciesId?.let(speciesMap::get),
                        null,
                        null,
                        campaignId,
                    )
                    val id = queries.lastInsertRowId().executeAsOne()
                    record.placeIds.mapNotNull(placeMap::get).forEach { database.npcLocationQueries.insert(id, it) }
                    records++
                    fresh += PhotoOwner.NPC to record.id
                    id
                }
        }

        private fun addQuest(record: FileRecord.Quest) {
            questMap[record.id] =
                matched(questsByName, record.title) ?: run {
                    val queries = database.questQueries
                    queries.insert(
                        nameForInsert(questsByName, record.title),
                        record.status,
                        record.reward,
                        record.giverNpcId?.let(npcMap::get),
                        record.placeId?.let(placeMap::get),
                        record.description,
                        campaignId,
                    )
                    records++
                    queries.lastInsertRowId().executeAsOne()
                }
        }

        private fun addParty(record: FileRecord.Party) {
            partyMap[record.id] =
                matched(partyByName, record.name) ?: run {
                    val queries = database.partyMemberQueries
                    val isPlayer = record.isPlayerCharacter && !playerTaken
                    playerTaken = playerTaken || isPlayer
                    queries.insert(
                        nameForInsert(partyByName, record.name),
                        record.characterClass,
                        record.race,
                        record.level?.toLong(),
                        if (isPlayer) record.playerName else "",
                        isPlayer,
                        record.presence,
                        record.hpMax?.toLong(),
                        record.hpCurrent?.toLong(),
                        record.description,
                        null,
                        null,
                        campaignId,
                    )
                    records++
                    fresh += PhotoOwner.PARTY to record.id
                    queries.lastInsertRowId().executeAsOne()
                }
        }

        private fun addSession(record: FileRecord.Session) {
            // the open session of the campaign stays the only one
            val endedAt = record.endedAt ?: record.date.takeIf { liveTaken }
            liveTaken = liveTaken || endedAt == null
            database.sessionNoteQueries.insert(record.title, record.date, endedAt, record.createdAt, campaignId)
            val id = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
            sessionMap[record.id] = id
            record.tags.distinct().forEach { name ->
                database.tagQueries.insert(name, campaignId)
                database.tagQueries.selectIdByName(name, campaignId).executeAsOneOrNull()?.let {
                    database.sessionTagQueries.insert(id, it)
                }
            }
            sessions++
        }

        private fun addEntry(record: FileRecord.Entry) {
            val sessionId = sessionMap[record.sessionId] ?: return
            val body =
                remapMentionTokens(record.body) { ref ->
                    when (ref.type) {
                        EntityKind.NPC -> npcMap[ref.id]
                        EntityKind.PARTY -> partyMap[ref.id]
                        EntityKind.LOCATION -> placeMap[ref.id]
                        EntityKind.QUEST -> questMap[ref.id]
                        EntityKind.SPECIES -> speciesMap[ref.id]
                    }
                }
            database.sessionEntryQueries.insert(sessionId, record.createdAt, body, record.edited, record.mentionCheck)
        }

        private fun addPhoto(record: FileRecord.Photo) {
            if ((record.owner to record.ownerId) !in fresh) return
            val bytes = decodeJpeg(record.data) ?: return
            when (record.owner) {
                PhotoOwner.SPECIES ->
                    speciesMap[record.ownerId]?.let {
                        database.speciesQueries.setPhoto(
                            bytes,
                            thumb(bytes, PhotoSlot.PORTRAIT),
                            it,
                        )
                    }
                PhotoOwner.PLACE ->
                    placeMap[record.ownerId]?.let {
                        database.locationQueries.setCover(
                            bytes,
                            thumb(bytes, PhotoSlot.COVER),
                            it,
                        )
                    }
                PhotoOwner.NPC ->
                    npcMap[record.ownerId]?.let {
                        database.npcQueries.setPortrait(
                            bytes,
                            thumb(bytes, PhotoSlot.PORTRAIT),
                            it,
                        )
                    }
                PhotoOwner.PARTY ->
                    partyMap[record.ownerId]?.let {
                        database.partyMemberQueries.setPortrait(
                            bytes,
                            thumb(bytes, PhotoSlot.PORTRAIT),
                            it,
                        )
                    }
            }
        }

        private fun thumb(
            full: ByteArray,
            slot: PhotoSlot,
        ): ByteArray = makeThumb(full, slot)

        // only a jpeg of a sane size is decoded, a picture that is not one is dropped and the record stays without it
        private fun decodeJpeg(text: String): ByteArray? {
            val bytes =
                try {
                    Base64.decode(text)
                } catch (_: IllegalArgumentException) {
                    throw ImportFailedException(ImportError.Malformed)
                }
            val (width, height) = jpegDimensions(bytes) ?: return null
            return bytes.takeIf { width <= MAX_PHOTO_SIDE_PX && height <= MAX_PHOTO_SIDE_PX }
        }
    }

    private fun namesOf(rows: List<Pair<String, Long>>): Map<String, Long> =
        rows.groupBy({ it.first.nameKey() }, { it.second }).mapValues { it.value.min() }
}
