package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.jpegDimensions
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.data.insertLive
import com.lyrismet.incadent.feature.campaign.domain.exchange.nameKey
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import kotlin.io.encoding.Base64
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

// a phone photo is cut to a few hundred pixels on save, a file that claims more than this is not one of ours
private const val MAX_PHOTO_SIDE_PX = 4000
private const val DEFAULT_CAMPAIGN_NAME = "Incadent"
private const val MAX_NAME_CHARS = 80
private const val COPY_SUFFIX = " (2)"
private const val MAX_PLACE_DEPTH = 1000

// a link the file leaves empty is cleared too, only one that cannot be followed keeps what the record has now
private fun link(
    fileId: Long?,
    map: Map<Long, Long>,
    current: Long?,
): Long? = if (fileId == null) null else map[fileId] ?: current

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
        val campaignId = targetCampaign(request.target, reader.header)
        val session = ImportSession(campaignId, request, reader.header.withPhotos)
        while (true) session.accept(reader.nextRecord() ?: break)
        return ImportResult(campaignId, session.records, session.sessions)
    }

    private fun targetCampaign(
        target: ImportTarget,
        header: FileHeader,
    ): String =
        when (target) {
            is ImportTarget.Existing -> target.campaignId
            is ImportTarget.NewCampaign -> {
                val name =
                    header.name.ifBlank { target.name }.trim().ifBlank { DEFAULT_CAMPAIGN_NAME }.take(
                        MAX_NAME_CHARS,
                    )
                database.campaignQueries.insertLive(name, originId = OriginSet().claim(header.originId))
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
        private val mapByKind =
            mapOf(
                EntityKind.NPC to npcMap,
                EntityKind.PARTY to partyMap,
                EntityKind.LOCATION to placeMap,
                EntityKind.QUEST to questMap,
                EntityKind.SPECIES to speciesMap,
            )

        private val fights = FightImport(database, campaignId, sessionMap, speciesMap, npcMap, partyMap)

        // only records written by this import take a photo, a matched one keeps what it has
        private val fresh = mutableSetOf<Pair<PhotoOwner, Long>>()

        private val copies = request.conflicts == ConflictMode.COPY
        private val existingSpecies = database.speciesQueries.selectAll(campaignId).executeAsList()
        private val speciesByName = namesOf(existingSpecies.map { it.name to it.id })
        private val existingPlaces = database.locationQueries.selectAll(campaignId).executeAsList()
        private val existingPlacesById = existingPlaces.associateBy { it.id }
        private val placeParents = existingPlaces.associate { it.id to it.parent_id }.toMutableMap()
        private val placesByName = namesOf(existingPlaces.map { it.name to it.id })
        private val existingNpcs = database.npcQueries.selectAll(campaignId).executeAsList()
        private val existingNpcsById = existingNpcs.associateBy { it.id }
        private val npcsByName = namesOf(existingNpcs.map { it.name to it.id })
        private val existingQuests = database.questQueries.selectAll(campaignId).executeAsList()
        private val existingQuestsById = existingQuests.associateBy { it.id }
        private val questsByName = namesOf(existingQuests.map { it.title to it.id })
        private val existingParty = database.partyMemberQueries.selectAll(campaignId).executeAsList()
        private val existingPartyById = existingParty.associateBy { it.id }
        private val partyByName = namesOf(existingParty.map { it.name to it.id })
        private var playerTaken = existingParty.any { it.is_player_character }
        private val existingSessions = database.sessionNoteQueries.selectAll(campaignId).executeAsList()
        private var liveTaken = existingSessions.any { it.ended_at == null }

        // a file's origin id is kept for a row the import adds unless the campaign already holds it
        private val origins =
            mapOf(
                ExchangeKind.SPECIES to OriginSet(existingSpecies.map { it.origin_id }),
                ExchangeKind.PLACE to OriginSet(existingPlaces.map { it.origin_id }),
                ExchangeKind.NPC to OriginSet(existingNpcs.map { it.origin_id }),
                ExchangeKind.QUEST to OriginSet(existingQuests.map { it.origin_id }),
                ExchangeKind.PARTY to OriginSet(existingParty.map { it.origin_id }),
                ExchangeKind.SESSION to OriginSet(existingSessions.map { it.origin_id }),
            )

        // entry ids only have to differ inside one session, and an imported session starts empty
        private val entryOrigins = mutableMapOf<Long, OriginSet>()

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
                is FightRecord -> fights.accept(record)
                is FileRecord.Photo -> addPhoto(record)
            }
        }

        // a record outside the pick is skipped, its notes and photo then find no owner and are skipped too
        private fun isWanted(record: FileRecord): Boolean =
            when (record) {
                is FileRecord.Species -> scope?.species?.contains(record.id) != false
                is FileRecord.Place -> scope?.places?.contains(record.id) != false
                is FileRecord.Npc -> scope?.npcs?.contains(record.id) != false
                is FileRecord.Quest -> scope?.quests?.contains(record.id) != false
                is FileRecord.Party -> scope?.party?.contains(record.id) != false
                is FileRecord.Session -> scope?.sessions?.contains(record.id) != false
                is FileRecord.Tag -> request.selection?.tags != false
                is FileRecord.Phrase -> request.selection?.phrases != false
                is FileRecord.Photo -> fileHasPhotos && request.selection?.photos != false
                is FileRecord.Entry -> true
                is FightRecord -> fights.isWanted(record, scope)
            }

        // a matched record is kept or overwritten, the file's own id then points at the one that was used
        private fun matched(
            ref: ExchangeRef,
            owner: PhotoOwner?,
            byName: Map<String, Long>,
            name: String,
            overwrite: (Long) -> Unit,
        ): Long? {
            val existing = byName[name.nameKey()]?.takeIf { !copies } ?: return null
            if (ref in request.replace) {
                overwrite(existing)
                records++
                // the photo of the file replaces the one there, a file without one leaves it alone
                owner?.let { fresh += it to ref.id }
            }
            return existing
        }

        // counts a record the import wrote, only those take a photo from the file
        private fun inserted(
            owner: PhotoOwner?,
            fileId: Long,
            id: Long,
        ): Long {
            records++
            owner?.let { fresh += it to fileId }
            return id
        }

        private fun nameForInsert(
            byName: Map<String, Long>,
            name: String,
        ): String = if (copies && name.nameKey() in byName) name + COPY_SUFFIX else name

        private fun addSpecies(record: FileRecord.Species) {
            speciesMap[record.id] =
                matched(ExchangeRef(ExchangeKind.SPECIES, record.id), PhotoOwner.SPECIES, speciesByName, record.name) {
                    database.speciesQueries.updateImported(
                        record.name,
                        record.creatureType,
                        record.vulnerabilities,
                        record.notes,
                        record.hitPoints?.toLong(),
                        it,
                    )
                } ?: run {
                    val queries = database.speciesQueries
                    queries.insert(
                        nameForInsert(speciesByName, record.name),
                        record.creatureType,
                        record.vulnerabilities,
                        record.notes,
                        record.hitPoints?.toLong(),
                        null,
                        null,
                        campaignId,
                        origins.getValue(ExchangeKind.SPECIES).claim(record.originId),
                    )
                    inserted(PhotoOwner.SPECIES, record.id, queries.lastInsertRowId().executeAsOne())
                }
        }

        private fun addPlace(record: FileRecord.Place) {
            placeMap[record.id] =
                matched(ExchangeRef(ExchangeKind.PLACE, record.id), PhotoOwner.PLACE, placesByName, record.name) {
                    overwritePlace(it, record)
                } ?: run {
                    val queries = database.locationQueries
                    val parent = record.parentId?.let(placeMap::get)
                    queries.insert(
                        nameForInsert(placesByName, record.name),
                        record.description,
                        parent,
                        null,
                        null,
                        campaignId,
                        origins.getValue(ExchangeKind.PLACE).claim(record.originId),
                    )
                    inserted(PhotoOwner.PLACE, record.id, queries.lastInsertRowId().executeAsOne()).also {
                        placeParents[it] = parent
                    }
                }
        }

        private fun addNpc(record: FileRecord.Npc) {
            npcMap[record.id] =
                matched(ExchangeRef(ExchangeKind.NPC, record.id), PhotoOwner.NPC, npcsByName, record.name) {
                    overwriteNpc(it, record)
                } ?: run {
                    val queries = database.npcQueries
                    queries.insert(
                        nameForInsert(npcsByName, record.name),
                        record.status,
                        record.description,
                        record.race,
                        record.lifeState,
                        record.nicknames,
                        record.speciesId?.let(speciesMap::get),
                        record.vulnerabilities,
                        null,
                        null,
                        campaignId,
                        origins.getValue(ExchangeKind.NPC).claim(record.originId),
                    )
                    val id = queries.lastInsertRowId().executeAsOne()
                    record.placeIds.mapNotNull(placeMap::get).forEach { database.npcLocationQueries.insert(id, it) }
                    inserted(PhotoOwner.NPC, record.id, id)
                }
        }

        private fun addQuest(record: FileRecord.Quest) {
            questMap[record.id] =
                matched(ExchangeRef(ExchangeKind.QUEST, record.id), null, questsByName, record.title) {
                    overwriteQuest(it, record)
                } ?: run {
                    val queries = database.questQueries
                    queries.insert(
                        nameForInsert(questsByName, record.title),
                        record.status,
                        record.reward,
                        record.giverNpcId?.let(npcMap::get),
                        record.placeId?.let(placeMap::get),
                        record.description,
                        campaignId,
                        origins.getValue(ExchangeKind.QUEST).claim(record.originId),
                    )
                    inserted(null, record.id, queries.lastInsertRowId().executeAsOne())
                }
        }

        private fun addParty(record: FileRecord.Party) {
            partyMap[record.id] =
                matched(ExchangeRef(ExchangeKind.PARTY, record.id), PhotoOwner.PARTY, partyByName, record.name) {
                    overwriteParty(it, record)
                } ?: run {
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
                        origins.getValue(ExchangeKind.PARTY).claim(record.originId),
                    )
                    val id = queries.lastInsertRowId().executeAsOne()
                    // the fight that felled them is not in the file's party record, so only the fact is kept
                    if (record.fallen) queries.setFallen(true, null, id)
                    inserted(PhotoOwner.PARTY, record.id, id)
                }
        }

        private fun overwritePlace(
            id: Long,
            record: FileRecord.Place,
        ) {
            val current = existingPlacesById.getValue(id)
            // a parent that would loop the places is not followed
            val parent = link(record.parentId, placeMap, current.parent_id)?.takeIf { !insideOf(it, id) }
            database.locationQueries.update(record.name, record.description, parent, id)
            placeParents[id] = parent
        }

        // true when [place] is [ancestor] or sits somewhere below it
        private fun insideOf(
            place: Long,
            ancestor: Long,
        ): Boolean {
            var step: Long? = place
            var guard = 0
            while (step != null && step != ancestor && guard++ < MAX_PLACE_DEPTH) {
                step = placeParents[step]
            }
            return step == ancestor
        }

        private fun overwriteNpc(
            id: Long,
            record: FileRecord.Npc,
        ) {
            val current = existingNpcsById.getValue(id)
            database.npcQueries.update(
                record.name,
                record.status,
                record.description,
                record.race,
                record.lifeState,
                record.nicknames,
                link(record.speciesId, speciesMap, current.species_id),
                record.vulnerabilities,
                id,
            )
            val places = record.placeIds.mapNotNull(placeMap::get)
            // places the file names but did not bring along leave the current ones alone, like link() does
            if (places.isEmpty() && record.placeIds.isNotEmpty()) return
            database.npcLocationQueries.deleteByNpc(id)
            places.forEach { database.npcLocationQueries.insert(id, it) }
        }

        private fun overwriteQuest(
            id: Long,
            record: FileRecord.Quest,
        ) {
            val current = existingQuestsById.getValue(id)
            database.questQueries.update(
                record.title,
                record.status,
                record.reward,
                link(record.giverNpcId, npcMap, current.given_by_npc_id),
                link(record.placeId, placeMap, current.location_id),
                record.description,
                id,
            )
        }

        // who plays the character and whether they are in the scene stay as the campaign has them
        private fun overwriteParty(
            id: Long,
            record: FileRecord.Party,
        ) {
            val current = existingPartyById.getValue(id)
            database.partyMemberQueries.update(
                record.name,
                record.characterClass,
                record.race,
                record.level?.toLong(),
                current.player_name,
                current.is_player_character,
                current.presence,
                record.hpMax?.toLong(),
                record.hpCurrent?.toLong(),
                record.description,
                id,
            )
        }

        private val tagIds = mutableMapOf<String, Long>()

        private fun tagId(name: String): Long? =
            tagIds[name] ?: run {
                database.tagQueries.insert(name, campaignId)
                database.tagQueries
                    .selectIdByName(name, campaignId)
                    .executeAsOneOrNull()
                    ?.also { tagIds[name] = it }
            }

        private fun addSession(record: FileRecord.Session) {
            // the open session of the campaign stays the only one
            val endedAt = record.endedAt ?: record.date.takeIf { liveTaken }
            liveTaken = liveTaken || endedAt == null
            database.sessionNoteQueries.insert(
                record.title,
                record.date,
                endedAt,
                record.createdAt,
                campaignId,
                origins.getValue(ExchangeKind.SESSION).claim(record.originId),
            )
            val id = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
            sessionMap[record.id] = id
            record.tags.distinct().forEach { name ->
                tagId(name)?.let { database.sessionTagQueries.insert(id, it) }
            }
            sessions++
        }

        private fun addEntry(record: FileRecord.Entry) {
            val sessionId = sessionMap[record.sessionId] ?: return
            val body =
                remapMentionTokens(record.body) { ref ->
                    mapByKind[ref.type]?.get(ref.id)
                }
            val origin = entryOrigins.getOrPut(sessionId) { OriginSet() }.claim(record.originId)
            database.sessionEntryQueries.insert(
                sessionId,
                record.createdAt,
                body,
                record.edited,
                record.mentionCheck,
                origin,
            )
        }

        private fun addPhoto(record: FileRecord.Photo) {
            if ((record.owner to record.ownerId) !in fresh) return
            val bytes = decodeJpeg(record.data) ?: return
            val slot = if (record.owner == PhotoOwner.PLACE) PhotoSlot.COVER else PhotoSlot.PORTRAIT
            val thumb = makeThumb(bytes, slot)
            when (record.owner) {
                PhotoOwner.SPECIES ->
                    speciesMap[record.ownerId]?.let {
                        database.speciesQueries.setPhoto(
                            bytes,
                            thumb,
                            it,
                        )
                    }
                PhotoOwner.PLACE ->
                    placeMap[record.ownerId]?.let {
                        database.locationQueries.setCover(
                            bytes,
                            thumb,
                            it,
                        )
                    }
                PhotoOwner.NPC -> npcMap[record.ownerId]?.let { database.npcQueries.setPortrait(bytes, thumb, it) }
                PhotoOwner.PARTY ->
                    partyMap[record.ownerId]?.let {
                        database.partyMemberQueries.setPortrait(
                            bytes,
                            thumb,
                            it,
                        )
                    }
            }
        }

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
