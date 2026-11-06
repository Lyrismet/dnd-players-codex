package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.jpegDimensions
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.data.insertLive
import com.lyrismet.incadent.feature.campaign.domain.exchange.RecordMatch
import com.lyrismet.incadent.feature.campaign.domain.exchange.nameKey
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
import com.lyrismet.incadent.feature.codex.domain.model.SheetRanges
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import com.lyrismet.incadent.persistence.creatureTypeIdIn
import kotlinx.serialization.builtins.ListSerializer
import kotlin.io.encoding.Base64
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

// a phone photo is cut to a few hundred pixels on save, a file that claims more than this is not one of ours
private const val MAX_PHOTO_SIDE_PX = 4000
private const val DEFAULT_CAMPAIGN_NAME = "Incadent"
private const val MAX_NAME_CHARS = 80
private const val COPY_SUFFIX = " (2)"
private const val MAX_PLACE_DEPTH = 1000
private const val SHEET_VERSION = 6

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
        // only an import into a campaign that was there before can be taken back
        val journal = ImportJournal(enabled = request.target is ImportTarget.Existing)
        val session = ImportSession(campaignId, request, reader.header, journal)
        while (true) session.accept(reader.nextRecord() ?: break)
        session.finish()
        journal.write(database, campaignId)
        return ImportResult(campaignId, session.records, session.sessions, session.partyAdded)
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

    // what to do with a record of the file once it is held up against the campaign
    private sealed interface Outcome {
        data class Use(
            val id: Long,
        ) : Outcome

        data object Insert : Outcome

        data object Skip : Outcome
    }

    @Suppress("LargeClass", "TooManyFunctions")
    private inner class ImportSession(
        private val campaignId: String,
        private val request: ImportRequest,
        private val header: FileHeader,
        private val journal: ImportJournal,
    ) {
        private val scope = request.scope
        private val fileHasPhotos = header.withPhotos
        var records = 0
        var sessions = 0
        var partyAdded = 0

        // a friend copy carries no vitals, a personal copy of someone else brings them only when asked
        private val vitals = header.mode == ExportMode.FRIEND || request.takeVitals
        private val notes = (header.mode == ExportMode.FRIEND && header.partyNotes) || request.takeVitals
        private val takeLabel = header.mode == ExportMode.SELF && request.takeAsMine

        // the sheet, the counters and what a character carries come with the mark, a file before them has none
        private val takeSheet = takeLabel
        private val takeExtras = header.mode == ExportMode.SELF && request.takeVitals && header.version >= SHEET_VERSION
        private val sheetTaken = mutableSetOf<Long>()
        private val partyJournaled = mutableSetOf<Long>()

        private val speciesMap = mutableMapOf<Long, Long>()
        private val placeMap = mutableMapOf<Long, Long>()
        private val npcMap = mutableMapOf<Long, Long>()
        private val questMap = mutableMapOf<Long, Long>()
        private val partyMap = mutableMapOf<Long, Long>()
        private val itemMap = mutableMapOf<Long, Long>()
        private val sessionMap = mutableMapOf<Long, Long>()
        private val mapByKind =
            mapOf(
                EntityKind.NPC to npcMap,
                EntityKind.PARTY to partyMap,
                EntityKind.LOCATION to placeMap,
                EntityKind.QUEST to questMap,
                EntityKind.SPECIES to speciesMap,
                EntityKind.ITEM to itemMap,
            )

        private val copies = request.conflicts == ConflictMode.COPY
        private val fights =
            FightImport(database, campaignId, sessionMap, speciesMap, npcMap, partyMap, journal, dedupe = !copies)

        // only records written by this import take a photo, a matched one keeps what it has
        private val fresh = mutableSetOf<Pair<PhotoOwner, Long>>()

        private val locals = CampaignLocals(database, campaignId)
        private val existingSpecies = locals.species
        private val existingSpeciesById = existingSpecies.associateBy { it.id }
        private val speciesByName = namesOf(existingSpecies.map { it.name to it.id })
        private val existingPlaces = locals.places
        private val existingPlacesById = existingPlaces.associateBy { it.id }
        private val placeParents = existingPlaces.associate { it.id to it.parent_id }.toMutableMap()
        private val placesByName = namesOf(existingPlaces.map { it.name to it.id })
        private val existingNpcs = locals.npcs
        private val existingNpcsById = existingNpcs.associateBy { it.id }
        private val npcsByName = namesOf(existingNpcs.map { it.name to it.id })
        private val existingQuests = locals.quests
        private val existingQuestsById = existingQuests.associateBy { it.id }
        private val questsByName = namesOf(existingQuests.map { it.title to it.id })
        private val existingParty = locals.party
        private val existingPartyById = existingParty.associateBy { it.id }
        private val partyByName = namesOf(existingParty.map { it.name to it.id })
        private val existingItems = locals.items
        private val existingItemsById = existingItems.associateBy { it.id }
        private var liveTaken = locals.sessions.any { it.ended_at == null }

        // a file's origin id is kept for a row the import adds unless the campaign already holds it
        private val origins =
            mapOf(
                ExchangeKind.SPECIES to OriginSet(existingSpecies.map { it.origin_id }),
                ExchangeKind.PLACE to OriginSet(existingPlaces.map { it.origin_id }),
                ExchangeKind.NPC to OriginSet(existingNpcs.map { it.origin_id }),
                ExchangeKind.QUEST to OriginSet(existingQuests.map { it.origin_id }),
                ExchangeKind.PARTY to OriginSet(existingParty.map { it.origin_id }),
                ExchangeKind.ITEM to OriginSet(existingItems.map { it.origin_id }),
                ExchangeKind.SESSION to OriginSet(locals.sessions.map { it.origin_id }),
            )

        // entry ids only have to differ inside one session
        private val entryOrigins = mutableMapOf<Long, OriginSet>()

        // the sessions of the file that went into a session already there, their notes are merged
        private val mergedSessions = mutableSetOf<Long>()
        private val overwritten = mutableSetOf<Pair<PhotoOwner, Long>>()

        // flat dispatch over the kinds of record, one branch each
        @Suppress("CyclomaticComplexMethod")
        fun accept(record: FileRecord) {
            if (!isWanted(record)) return
            when (record) {
                is FileRecord.Species -> addSpecies(record)
                is FileRecord.Place -> addPlace(record)
                is FileRecord.Npc -> addNpc(record)
                is FileRecord.Quest -> addQuest(record)
                is FileRecord.Party -> addParty(record)
                is FileRecord.Item -> addItem(record)
                is FileRecord.Sheet, is FileRecord.Resource -> addSheetRecord(record)
                is FileRecord.Tag -> database.tagQueries.insert(record.name, campaignId)
                is FileRecord.Phrase -> database.phraseSnippetQueries.insert(record.text, campaignId)
                is FileRecord.Session -> addSession(record)
                is FileRecord.Entry -> addEntry(record)
                is FightRecord -> fights.accept(record)
                is FileRecord.Photo -> addPhoto(record)
            }
        }

        // a record outside the pick is skipped, its notes and photo then find no owner and are skipped too
        @Suppress("CyclomaticComplexMethod")
        private fun isWanted(record: FileRecord): Boolean =
            when (record) {
                is FileRecord.Species -> scope?.species?.contains(record.id) != false
                is FileRecord.Place -> scope?.places?.contains(record.id) != false
                is FileRecord.Npc -> scope?.npcs?.contains(record.id) != false
                is FileRecord.Quest -> scope?.quests?.contains(record.id) != false
                is FileRecord.Party -> scope?.party?.contains(record.id) != false
                is FileRecord.Item -> scope?.items?.contains(record.id) != false
                is FileRecord.Sheet -> takeSheet && scope?.party?.contains(record.partyId) != false
                is FileRecord.Resource -> takeSheet && scope?.party?.contains(record.partyId) != false
                is FileRecord.Session -> scope?.sessions?.contains(record.id) != false
                is FileRecord.Tag -> request.selection?.tags != false
                is FileRecord.Phrase -> request.selection?.phrases != false
                is FileRecord.Photo -> fileHasPhotos && request.selection?.photos != false
                is FileRecord.Entry -> true
                is FightRecord -> fights.isWanted(record, scope)
            }

        // one record of the file that may be one already there: how to tell, and how to write it over
        @Suppress("LongParameterList")
        private inner class Candidate(
            val kind: ExchangeKind,
            val fileId: Long,
            val originId: String?,
            val name: String,
            val owner: PhotoOwner?,
            val diff: (Long) -> List<FieldChange>,
            val overwrite: (Long, FieldPick) -> Unit,
        )

        // the file's own id then points at the record that was used, kept as it is or overwritten
        private fun decide(c: Candidate): Outcome {
            if (copies) return Outcome.Insert
            val ref = ExchangeRef(c.kind, c.fileId)
            return when (val found = locals.match(c.kind, c.originId, c.name)) {
                is RecordMatch.ByOrigin -> use(c, ref, found.id)
                is RecordMatch.ByName ->
                    if (request.mergeNameTwins) {
                        alias(c.kind, c.originId, found.id)
                        use(c, ref, found.id)
                    } else {
                        Outcome.Insert
                    }
                is RecordMatch.Similar ->
                    when (request.resolutions[ref]) {
                        Resolution.SAME -> {
                            alias(c.kind, c.originId, found.id)
                            use(c, ref, found.id)
                        }
                        Resolution.APART -> apart(c.kind, c.originId, found.id)
                        else -> Outcome.Skip
                    }
                RecordMatch.None ->
                    if (locals.isDeleted(c.kind, c.originId) &&
                        ref !in request.restore
                    ) {
                        Outcome.Skip
                    } else {
                        Outcome.Insert
                    }
            }
        }

        // only what the person took is written, and the state before it goes to the journal
        private fun use(
            c: Candidate,
            ref: ExchangeRef,
            id: Long,
        ): Outcome {
            val pick = FieldPick(request.fields[ref].orEmpty(), ref in request.replace, request.fromFileLabel)
            if (c.diff(id).any(pick::takes)) {
                journal.changed(c.kind.name, id, before(c.kind, id))
                c.overwrite(id, pick)
                records++
                // the photo of the file replaces the one there, a file without one leaves it alone
                c.owner?.let {
                    fresh += it to ref.id
                    overwritten += it to ref.id
                }
            }
            return Outcome.Use(id)
        }

        private fun apart(
            kind: ExchangeKind,
            fileOrigin: String?,
            localId: Long,
        ): Outcome {
            val local = locals.originOf(kind, localId)
            if (!fileOrigin.isNullOrBlank() && local != null) {
                database.keptApartQueries.insert(campaignId, kind, fileOrigin, local)
            }
            return Outcome.Insert
        }

        // the next file from the same player carries the old id, which now means the record it was merged into
        private fun alias(
            kind: ExchangeKind,
            fileOrigin: String?,
            localId: Long,
        ) {
            val local = locals.originOf(kind, localId)
            if (fileOrigin.isNullOrBlank() || local == null || fileOrigin == local) return
            database.originAliasQueries.repoint(local, campaignId, kind, fileOrigin)
            database.originAliasQueries.insert(campaignId, kind, fileOrigin, local)
            journal.alias(kind.name, fileOrigin)
        }

        // counts a record the import wrote, only those take a photo from the file
        @Suppress("LongParameterList")
        private fun inserted(
            kind: ExchangeKind,
            text: String,
            owner: PhotoOwner?,
            fileId: Long,
            id: Long,
            originId: String,
        ): Long {
            records++
            owner?.let { fresh += it to fileId }
            val brought = ExchangeRef(kind, fileId) in request.restore
            journal.added(kind.name, id, originId, brought)
            later(kind, id, text)
            return id
        }

        private fun nameForInsert(
            byName: Map<String, Long>,
            name: String,
        ): String = if ((copies || !request.mergeNameTwins) && name.nameKey() in byName) name + COPY_SUFFIX else name

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        private fun before(
            kind: ExchangeKind,
            id: Long,
        ): FileRecord =
            when (kind) {
                ExchangeKind.SPECIES ->
                    existingSpeciesById.getValue(id).let {
                        FileRecord.Species(
                            id,
                            it.name,
                            it.creature_type,
                            it.vulnerabilities,
                            it.notes,
                            it.hit_points?.toInt(),
                            customType = it.custom_type_name,
                        )
                    }
                ExchangeKind.PLACE ->
                    existingPlacesById.getValue(id).let { FileRecord.Place(id, it.name, it.description, it.parent_id) }
                ExchangeKind.NPC ->
                    existingNpcsById.getValue(id).let {
                        FileRecord.Npc(
                            id,
                            it.name,
                            it.status,
                            it.description,
                            it.race,
                            it.life_state,
                            it.nicknames,
                            it.species_id,
                            it.vulnerabilities,
                            it.location_ids.toIdList(),
                            characterClass = it.character_class,
                            subclass = it.subclass,
                        )
                    }
                ExchangeKind.QUEST ->
                    existingQuestsById.getValue(id).let {
                        FileRecord.Quest(
                            id,
                            it.title,
                            it.status,
                            it.reward,
                            it.given_by_npc_id,
                            it.location_id,
                            it.description,
                        )
                    }
                ExchangeKind.PARTY ->
                    existingPartyById.getValue(id).let {
                        FileRecord
                            .Party(
                                id,
                                it.name,
                                it.character_class,
                                it.race,
                                it.level?.toInt(),
                                it.player_name,
                                it.is_player_character,
                                it.presence,
                                it.hp_max?.toInt(),
                                it.hp_current?.toInt(),
                                it.description,
                                it.fallen,
                                subclass = it.subclass,
                            ).withVitals(id)
                    }
                ExchangeKind.ITEM ->
                    existingItemsById.getValue(id).let {
                        val owner = it.owner_kind?.toExchangeKind()
                        FileRecord.Item(
                            id,
                            it.name,
                            it.quantity.toInt(),
                            it.price?.toInt(),
                            it.coin,
                            owner,
                            owner?.let { _ -> it.owner_id },
                            it.notes,
                        )
                    }
                ExchangeKind.SESSION -> error("a session is never overwritten")
            }

        private fun addSpecies(record: FileRecord.Species) {
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.SPECIES,
                        record.id,
                        record.originId,
                        record.name,
                        PhotoOwner.SPECIES,
                        { diffSpecies(record, existingSpeciesById.getValue(it)) },
                    ) { id, pick -> overwriteSpecies(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> speciesMap[record.id] = outcome.id
                Outcome.Skip -> Unit
                Outcome.Insert -> {
                    val queries = database.speciesQueries
                    val origin = origins.getValue(ExchangeKind.SPECIES).claim(record.originId)
                    queries.insert(
                        nameForInsert(speciesByName, record.name),
                        record.creatureType,
                        record.vulnerabilities,
                        record.notes,
                        record.hitPoints?.toLong(),
                        null,
                        null,
                        campaignId,
                        origin,
                        database.creatureTypeIdIn(campaignId, record.customType),
                    )
                    speciesMap[record.id] =
                        inserted(
                            ExchangeKind.SPECIES,
                            record.notes,
                            PhotoOwner.SPECIES,
                            record.id,
                            queries.lastInsertRowId().executeAsOne(),
                            origin,
                        )
                }
            }
        }

        private fun addPlace(record: FileRecord.Place) {
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.PLACE,
                        record.id,
                        record.originId,
                        record.name,
                        PhotoOwner.PLACE,
                        { diffPlace(record, existingPlacesById.getValue(it)) },
                    ) { id, pick -> overwritePlace(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> placeMap[record.id] = outcome.id
                Outcome.Skip -> Unit
                Outcome.Insert -> {
                    val queries = database.locationQueries
                    val parent = record.parentId?.let(placeMap::get)
                    val origin = origins.getValue(ExchangeKind.PLACE).claim(record.originId)
                    queries.insert(
                        nameForInsert(placesByName, record.name),
                        record.description,
                        parent,
                        null,
                        null,
                        campaignId,
                        origin,
                    )
                    placeMap[record.id] =
                        inserted(
                            ExchangeKind.PLACE,
                            record.description,
                            PhotoOwner.PLACE,
                            record.id,
                            queries.lastInsertRowId().executeAsOne(),
                            origin,
                        ).also { placeParents[it] = parent }
                }
            }
        }

        private fun addNpc(record: FileRecord.Npc) {
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.NPC,
                        record.id,
                        record.originId,
                        record.name,
                        PhotoOwner.NPC,
                        { diffNpc(record, existingNpcsById.getValue(it)) },
                    ) { id, pick -> overwriteNpc(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> npcMap[record.id] = outcome.id
                Outcome.Skip -> Unit
                Outcome.Insert -> {
                    val queries = database.npcQueries
                    val origin = origins.getValue(ExchangeKind.NPC).claim(record.originId)
                    queries.insert(
                        nameForInsert(npcsByName, record.name),
                        record.status,
                        record.description,
                        record.race,
                        record.characterClass,
                        record.subclass,
                        record.lifeState,
                        record.nicknames,
                        record.speciesId?.let(speciesMap::get),
                        record.vulnerabilities,
                        null,
                        null,
                        campaignId,
                        origin,
                    )
                    val id = queries.lastInsertRowId().executeAsOne()
                    record.placeIds.mapNotNull(placeMap::get).forEach { database.npcLocationQueries.insert(id, it) }
                    npcMap[record.id] =
                        inserted(
                            ExchangeKind.NPC,
                            record.description,
                            PhotoOwner.NPC,
                            record.id,
                            id,
                            origin,
                        )
                }
            }
        }

        private fun addQuest(record: FileRecord.Quest) {
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.QUEST,
                        record.id,
                        record.originId,
                        record.title,
                        null,
                        { diffQuest(record, existingQuestsById.getValue(it)) },
                    ) { id, pick -> overwriteQuest(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> questMap[record.id] = outcome.id
                Outcome.Skip -> Unit
                Outcome.Insert -> {
                    val queries = database.questQueries
                    val origin = origins.getValue(ExchangeKind.QUEST).claim(record.originId)
                    queries.insert(
                        nameForInsert(questsByName, record.title),
                        record.status,
                        record.reward,
                        record.giverNpcId?.let(npcMap::get),
                        record.placeId?.let(placeMap::get),
                        record.description,
                        campaignId,
                        origin,
                    )
                    questMap[record.id] =
                        inserted(
                            ExchangeKind.QUEST,
                            record.description,
                            null,
                            record.id,
                            queries.lastInsertRowId().executeAsOne(),
                            origin,
                        )
                }
            }
        }

        private fun addParty(record: FileRecord.Party) {
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.PARTY,
                        record.id,
                        record.originId,
                        record.name,
                        PhotoOwner.PARTY,
                        { diffParty(record, existingPartyById.getValue(it), header.partyNotes) },
                    ) { id, pick -> overwriteParty(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> {
                    partyMap[record.id] = outcome.id
                    if (takeExtras || (takeLabel && record.isPlayerCharacter)) {
                        // once per member, the first state is the one an undo goes back to
                        if (partyJournaled.add(outcome.id)) {
                            journal.changed(ExchangeKind.PARTY.name, outcome.id, before(ExchangeKind.PARTY, outcome.id))
                        }
                        takeExtras(outcome.id, record)
                        if (takeLabel && record.isPlayerCharacter) markAsMine(outcome.id, record.playerName)
                    }
                }
                Outcome.Skip -> Unit
                Outcome.Insert -> insertParty(record)
            }
        }

        private fun addItem(record: FileRecord.Item) {
            // what a character of the file carries is not written when that character did not come along
            if (record.ownerKind == ExchangeKind.PARTY && ownerOf(record) == null) return
            // what a character carries is theirs, it comes with the character and not with the records
            if (header.mode == ExportMode.SELF && record.ownerKind == ExchangeKind.PARTY && !takeSheet) return
            val outcome =
                decide(
                    Candidate(
                        ExchangeKind.ITEM,
                        record.id,
                        record.originId,
                        record.name,
                        PhotoOwner.ITEM,
                        { diffItem(record, existingItemsById.getValue(it)) },
                    ) { id, pick -> overwriteItem(id, record, pick) },
                )
            when (outcome) {
                is Outcome.Use -> itemMap[record.id] = outcome.id
                Outcome.Skip -> Unit
                Outcome.Insert -> {
                    val queries = database.itemQueries
                    val origin = origins.getValue(ExchangeKind.ITEM).claim(record.originId)
                    val owner = ownerOf(record)
                    queries.insert(
                        record.name,
                        record.quantity.toLong(),
                        record.price?.toLong(),
                        record.coin,
                        owner?.first,
                        owner?.second ?: 0L,
                        record.notes,
                        null,
                        null,
                        campaignId,
                        origin,
                    )
                    itemMap[record.id] =
                        inserted(
                            ExchangeKind.ITEM,
                            record.notes,
                            PhotoOwner.ITEM,
                            record.id,
                            queries.lastInsertRowId().executeAsOne(),
                            origin,
                        )
                }
            }
        }

        // who carries the item here, null when it is shared or its carrier was not written by this import
        private fun ownerOf(record: FileRecord.Item): Pair<EntityKind, Long>? {
            val fileId = record.ownerId ?: return null
            return when (record.ownerKind) {
                ExchangeKind.NPC -> npcMap[fileId]?.let { EntityKind.NPC to it }
                ExchangeKind.PLACE -> placeMap[fileId]?.let { EntityKind.LOCATION to it }
                ExchangeKind.PARTY -> partyMap[fileId]?.let { EntityKind.PARTY to it }
                else -> null
            }
        }

        private fun insertParty(record: FileRecord.Party) {
            val queries = database.partyMemberQueries
            val origin = origins.getValue(ExchangeKind.PARTY).claim(record.originId)
            queries.insert(
                nameForInsert(partyByName, record.name),
                record.characterClass,
                record.subclass,
                record.race,
                record.level?.toLong(),
                record.playerName,
                false,
                record.presence,
                record.hpMax?.toLong()?.takeIf { vitals },
                record.hpCurrent?.toLong()?.takeIf { vitals },
                record.description.takeIf { notes }.orEmpty(),
                null,
                null,
                campaignId,
                origin,
            )
            partyAdded++
            val id = queries.lastInsertRowId().executeAsOne()
            // the fight that felled them is not in the file's party record, so only the fact is kept
            if (record.fallen) queries.setFallen(true, null, id)
            takeExtras(id, record)
            partyMap[record.id] =
                inserted(
                    ExchangeKind.PARTY,
                    record.description.takeIf { notes }.orEmpty(),
                    PhotoOwner.PARTY,
                    record.id,
                    id,
                    origin,
                )
            if (takeLabel && record.isPlayerCharacter) markAsMine(id, record.playerName)
        }

        // a carrier the file names but did not bring along leaves the current one alone, like link() does
        private fun overwriteItem(
            id: Long,
            record: FileRecord.Item,
            pick: FieldPick,
        ) {
            val current = existingItemsById.getValue(id)
            val owner = ownerOf(record)
            val price = pick.number(RecordField.PRICE, current.price?.toInt(), record.price)?.toLong()
            database.itemQueries.update(
                pick.text(RecordField.NAME, current.name, record.name),
                pick.number(RecordField.QUANTITY, current.quantity.toInt(), record.quantity)?.toLong()
                    ?: current.quantity,
                price,
                if (price != null && price == record.price?.toLong()) record.coin else current.coin,
                owner?.first ?: current.owner_kind,
                owner?.second ?: current.owner_id,
                pick.text(RecordField.NOTES, current.notes, record.notes).also {
                    took(ExchangeKind.ITEM, id, current.notes, it, record.notes)
                },
                id,
            )
        }

        private fun FileRecord.Party.withVitals(id: Long): FileRecord.Party {
            val vitals = database.partyMemberQueries.selectVitals(id).executeAsOne()
            return copy(
                tempHp = vitals.temp_hp.toInt(),
                armorClass = vitals.armor_class?.toInt(),
                armorClassFrom = vitals.armor_class_from,
                deathSuccesses = vitals.death_saves_success.toInt(),
                deathFailures = vitals.death_saves_failure.toInt(),
            )
        }

        // the numbers beside the hit points of a personal copy, a missing armor class leaves the one there
        private fun takeExtras(
            id: Long,
            record: FileRecord.Party,
        ) {
            if (!takeExtras) return
            val queries = database.partyMemberQueries
            val current = queries.selectVitals(id).executeAsOne()
            queries.setVitals(
                current.hp_current,
                record.tempHp.coerceIn(SheetRanges.tempHp).toLong(),
                record.armorClass?.coerceIn(SheetRanges.armorClass)?.toLong() ?: current.armor_class,
                if (record.armorClass != null) record.armorClassFrom else current.armor_class_from,
                record.deathSuccesses.coerceIn(SheetRanges.deathSaves).toLong(),
                record.deathFailures.coerceIn(SheetRanges.deathSaves).toLong(),
                id,
            )
        }

        // a sheet of the file replaces the one there with its counters, the old ones go to the journal
        private fun addSheetRecord(record: FileRecord) {
            val partyNumber =
                when (record) {
                    is FileRecord.Sheet -> record.partyId
                    is FileRecord.Resource -> record.partyId
                    else -> return
                }
            val memberId = partyMap[partyNumber] ?: return
            if (sheetTaken.add(memberId)) {
                if (memberId in existingPartyById) {
                    val before = readSheetRecords(database, memberId, memberId)
                    journal.sheet(memberId, FileJson.encodeToString(ListSerializer(FileRecord.serializer()), before))
                }
                clearSheet(database, memberId)
            }
            writeSheetRecord(database, campaignId, memberId, record)
        }

        // the mark "я" is one per campaign, the one it leaves is put back when the import is taken back
        private fun markAsMine(
            id: Long,
            playerName: String,
        ) {
            val queries = database.partyMemberQueries
            val previous = existingParty.firstOrNull { it.is_player_character && it.id != id }?.id ?: 0L
            queries.clearOtherPlayerCharacters(id, campaignId)
            val row = queries.selectById(id).executeAsOne()
            queries.update(
                row.name,
                row.character_class,
                row.subclass,
                row.race,
                row.level,
                playerName.ifBlank { row.player_name },
                true,
                row.presence,
                row.hp_max,
                row.hp_current,
                row.description,
                id,
            )
            if (previous != 0L) journal.label(previous)
        }

        private fun overwriteSpecies(
            id: Long,
            record: FileRecord.Species,
            pick: FieldPick,
        ) {
            val current = existingSpeciesById.getValue(id)
            val takeType = !pick.value(RecordField.CREATURE_TYPE, false, true)
            database.speciesQueries.updateImported(
                pick.text(RecordField.NAME, current.name, record.name),
                if (takeType) current.creature_type else record.creatureType,
                if (takeType) current.custom_type_id else database.creatureTypeIdIn(campaignId, record.customType),
                pick.text(RecordField.VULNERABILITIES, current.vulnerabilities, record.vulnerabilities),
                pick.text(RecordField.NOTES, current.notes, record.notes).also {
                    took(ExchangeKind.SPECIES, id, current.notes, it, record.notes)
                },
                pick.number(RecordField.HIT_POINTS, current.hit_points?.toInt(), record.hitPoints)?.toLong(),
                id,
            )
        }

        private fun overwritePlace(
            id: Long,
            record: FileRecord.Place,
            pick: FieldPick,
        ) {
            val current = existingPlacesById.getValue(id)
            // a parent that would loop the places is not followed
            val parent = link(record.parentId, placeMap, current.parent_id)?.takeIf { !insideOf(it, id) }
            database.locationQueries.update(
                pick.text(RecordField.NAME, current.name, record.name),
                pick.text(RecordField.DESCRIPTION, current.description, record.description).also {
                    took(ExchangeKind.PLACE, id, current.description, it, record.description)
                },
                parent,
                id,
            )
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
            pick: FieldPick,
        ) {
            val current = existingNpcsById.getValue(id)
            database.npcQueries.update(
                pick.text(RecordField.NAME, current.name, record.name),
                pick.value(RecordField.STATUS, current.status, record.status),
                pick.text(RecordField.DESCRIPTION, current.description, record.description).also {
                    took(ExchangeKind.NPC, id, current.description, it, record.description)
                },
                pick.text(RecordField.RACE, current.race, record.race),
                pick.text(RecordField.CHARACTER_CLASS, current.character_class, record.characterClass),
                pick.text(RecordField.SUBCLASS, current.subclass, record.subclass),
                pick.value(RecordField.LIFE_STATE, current.life_state, record.lifeState),
                pick.text(RecordField.NICKNAMES, current.nicknames, record.nicknames),
                link(record.speciesId, speciesMap, current.species_id),
                pick.text(RecordField.VULNERABILITIES, current.vulnerabilities, record.vulnerabilities),
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
            pick: FieldPick,
        ) {
            val current = existingQuestsById.getValue(id)
            database.questQueries.update(
                pick.text(RecordField.NAME, current.title, record.title),
                pick.value(RecordField.STATUS, current.status, record.status),
                pick.text(RecordField.REWARD, current.reward, record.reward),
                link(record.giverNpcId, npcMap, current.given_by_npc_id),
                link(record.placeId, placeMap, current.location_id),
                pick.text(RecordField.DESCRIPTION, current.description, record.description).also {
                    took(ExchangeKind.QUEST, id, current.description, it, record.description)
                },
                id,
            )
        }

        // who plays, the mark and the scene stay as the campaign has them, and so does what the file lacks
        private fun overwriteParty(
            id: Long,
            record: FileRecord.Party,
            pick: FieldPick,
        ) {
            val current = existingPartyById.getValue(id)
            partyJournaled += id
            takeExtras(id, record)
            database.partyMemberQueries.update(
                pick.text(RecordField.NAME, current.name, record.name),
                pick.text(RecordField.CHARACTER_CLASS, current.character_class, record.characterClass),
                pick.text(RecordField.SUBCLASS, current.subclass, record.subclass),
                pick.text(RecordField.RACE, current.race, record.race),
                pick.number(RecordField.LEVEL, current.level?.toInt(), record.level)?.toLong(),
                current.player_name,
                current.is_player_character,
                current.presence,
                (record.hpMax?.toLong()).takeIf { vitals } ?: current.hp_max,
                (record.hpCurrent?.toLong()).takeIf { vitals } ?: current.hp_current,
                if (notes) {
                    pick.text(RecordField.DESCRIPTION, current.description, record.description).also {
                        took(ExchangeKind.PARTY, id, current.description, it, record.description)
                    }
                } else {
                    current.description
                },
                id,
            )
        }

        // sqlite NOCASE only folds ascii, so tags are matched in kotlin like TagRepositoryImpl does
        // a text of the file carries mentions by the file's numbers, they move to the new ids once every record is in
        private inner class PendingText(
            val kind: ExchangeKind,
            val id: Long,
            val raw: String,
        )

        private val pendingTexts = mutableListOf<PendingText>()

        private fun later(
            kind: ExchangeKind,
            id: Long,
            raw: String,
        ) {
            if (raw.contains("@[")) pendingTexts += PendingText(kind, id, raw)
        }

        // a text that now holds the file's words is one whose mentions are still the file's
        private fun took(
            kind: ExchangeKind,
            id: Long,
            before: String,
            after: String,
            raw: String,
        ) {
            if (after != before) later(kind, id, raw)
        }

        fun finish() = pendingTexts.forEach(::pointMentions)

        // only the part that came from the file is moved, what was there before keeps its own ids
        private fun pointMentions(text: PendingText) {
            val fixed = remapMentionTokens(text.raw) { mapByKind[it.type]?.get(it.id) }
            val current = readText(text)
            val at = current.lastIndexOf(text.raw)
            if (fixed != text.raw && at >= 0) writeText(text, current.replaceRange(at, at + text.raw.length, fixed))
        }

        private fun readText(text: PendingText): String =
            when (text.kind) {
                ExchangeKind.SPECIES -> database.speciesQueries.selectNotes(text.id).executeAsOne()
                ExchangeKind.PLACE -> database.locationQueries.selectDescription(text.id).executeAsOne()
                ExchangeKind.NPC -> database.npcQueries.selectDescription(text.id).executeAsOne()
                ExchangeKind.QUEST -> database.questQueries.selectDescription(text.id).executeAsOne()
                ExchangeKind.PARTY -> database.partyMemberQueries.selectDescription(text.id).executeAsOne()
                ExchangeKind.ITEM -> database.itemQueries.selectNotes(text.id).executeAsOne()
                ExchangeKind.SESSION -> ""
            }

        private fun writeText(
            text: PendingText,
            value: String,
        ) {
            when (text.kind) {
                ExchangeKind.SPECIES -> database.speciesQueries.setNotes(value, text.id)
                ExchangeKind.PLACE -> database.locationQueries.setDescription(value, text.id)
                ExchangeKind.NPC -> database.npcQueries.setDescription(value, text.id)
                ExchangeKind.QUEST -> database.questQueries.setDescription(value, text.id)
                ExchangeKind.PARTY -> database.partyMemberQueries.setDescription(value, text.id)
                ExchangeKind.ITEM -> database.itemQueries.setNotes(value, text.id)
                ExchangeKind.SESSION -> Unit
            }
        }

        private val tagIds: MutableMap<String, Long> by lazy {
            database.tagQueries
                .selectAll(campaignId)
                .executeAsList()
                .associateTo(mutableMapOf()) { it.name.lowercase() to it.id }
        }

        private fun tagId(name: String): Long? {
            val key = name.lowercase()
            tagIds[key]?.let { return it }
            database.tagQueries.insert(name, campaignId)
            val created =
                database.tagQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?.id
            return created?.also { tagIds[key] = it }
        }

        private fun addSession(record: FileRecord.Session) {
            val ref = ExchangeRef(ExchangeKind.SESSION, record.id)
            val day = record.date.date
            val twin = locals.sessionTwin(record.originId, record.title, day)
            when {
                twin == null &&
                    locals.isDeleted(
                        ExchangeKind.SESSION,
                        record.originId,
                    ) &&
                    ref !in request.restore -> Unit
                twin == null || copies -> insertSession(record, record.title, ref in request.restore)
                record.id in request.separateSessions ->
                    insertSession(
                        record,
                        listOf(record.title, request.fromFileLabel).filter { it.isNotBlank() }.joinToString(" · "),
                        false,
                    )
                else -> {
                    sessionMap[record.id] = twin.id
                    mergedSessions += record.id
                    alias(ExchangeKind.SESSION, record.originId, twin.id)
                }
            }
        }

        private fun insertSession(
            record: FileRecord.Session,
            title: String,
            brought: Boolean,
        ) {
            // the open session of the campaign stays the only one
            val endedAt = record.endedAt ?: record.date.takeIf { liveTaken }
            liveTaken = liveTaken || endedAt == null
            val origin = origins.getValue(ExchangeKind.SESSION).claim(record.originId)
            database.sessionNoteQueries.insert(
                title,
                record.date,
                endedAt,
                record.createdAt,
                campaignId,
                origin,
            )
            val id = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
            sessionMap[record.id] = id
            journal.added(ExchangeKind.SESSION.name, id, origin, brought)
            record.tags.distinct().forEach { name ->
                tagId(name)?.let { database.sessionTagQueries.insert(id, it) }
            }
            sessions++
        }

        private fun addEntry(record: FileRecord.Entry) {
            val sessionId = sessionMap[record.sessionId] ?: return
            val merged = record.sessionId in mergedSessions
            val held = if (merged) locals.entries[sessionId].orEmpty() else emptyList()
            if (alreadyHeld(record, held)) return
            val body =
                remapMentionTokens(record.body) { ref ->
                    mapByKind[ref.type]?.get(ref.id)
                }
            val origin =
                entryOrigins
                    .getOrPut(
                        sessionId,
                    ) { OriginSet(held.map { it.origin_id }) }
                    .claim(record.originId)
            database.sessionEntryQueries.insert(
                sessionId,
                record.createdAt,
                body,
                record.edited,
                record.mentionCheck,
                origin,
            )
            if (merged) {
                journal.entry(database.sessionEntryQueries.lastInsertRowId().executeAsOne())
                records++
            }
        }

        // a note the session already holds comes back with every import of the same file
        private fun alreadyHeld(
            record: FileRecord.Entry,
            held: List<Session_entry>,
        ): Boolean =
            if (record.originId.isNullOrBlank()) {
                held.any { it.created_at == record.createdAt && it.body == record.body }
            } else {
                held.any { it.origin_id == record.originId }
            }

        private fun addPhoto(record: FileRecord.Photo) {
            if ((record.owner to record.ownerId) !in fresh) return
            val bytes = decodeJpeg(record.data) ?: return
            val slot = if (record.owner == PhotoOwner.PLACE) PhotoSlot.COVER else PhotoSlot.PORTRAIT
            val thumb = makeThumb(bytes, slot)
            rememberPhoto(record)
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
                PhotoOwner.ITEM -> itemMap[record.ownerId]?.let { database.itemQueries.setPhoto(bytes, thumb, it) }
            }
        }

        // a photo that replaces another one is kept in the journal, so taking the import back can restore it
        private fun rememberPhoto(record: FileRecord.Photo) {
            if ((record.owner to record.ownerId) !in overwritten) return
            val local =
                when (record.owner) {
                    PhotoOwner.SPECIES ->
                        speciesMap[record.ownerId]?.let {
                            database.speciesQueries
                                .selectPhoto(it)
                                .executeAsOneOrNull()
                                ?.photo to
                                it
                        }
                    PhotoOwner.PLACE ->
                        placeMap[record.ownerId]?.let {
                            database.locationQueries
                                .selectPhoto(it)
                                .executeAsOneOrNull()
                                ?.cover to
                                it
                        }
                    PhotoOwner.NPC ->
                        npcMap[record.ownerId]?.let {
                            database.npcQueries
                                .selectPhoto(it)
                                .executeAsOneOrNull()
                                ?.portrait to
                                it
                        }
                    PhotoOwner.PARTY ->
                        partyMap[record.ownerId]?.let {
                            database.partyMemberQueries
                                .selectPhoto(it)
                                .executeAsOneOrNull()
                                ?.portrait to
                                it
                        }
                    PhotoOwner.ITEM ->
                        itemMap[record.ownerId]?.let {
                            database.itemQueries
                                .selectPhoto(it)
                                .executeAsOneOrNull()
                                ?.photo to it
                        }
                } ?: return
            journal.photo(record.owner, local.second, local.first?.let { Base64.encode(it) })
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
