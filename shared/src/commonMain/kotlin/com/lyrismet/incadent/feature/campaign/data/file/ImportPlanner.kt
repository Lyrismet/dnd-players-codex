package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.exchange.RecordMatch
import com.lyrismet.incadent.feature.campaign.domain.exchange.noteOverlap
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.PersonalCopy
import com.lyrismet.incadent.feature.campaign.domain.model.PlannedRecord
import com.lyrismet.incadent.feature.campaign.domain.model.PlannedSession
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.RecordStatus
import com.lyrismet.incadent.feature.campaign.domain.model.SessionStatus

/** holds a file up against a campaign and says what each record of it would be, nothing is written */
internal class ImportPlanner(
    private val database: AppDatabase,
) {
    fun plan(
        origin: FileOrigin,
        campaignId: String,
    ): ImportPlan {
        val source = origin.open()
        try {
            val reader = CampaignFileReader.open(source)
            val file = mutableListOf<FileRecord>()
            while (true) file += reader.nextRecord(skipPhotos = true) ?: break
            return build(reader.header, file, CampaignLocals(database, campaignId))
        } finally {
            source.close()
        }
    }

    private fun build(
        header: FileHeader,
        file: List<FileRecord>,
        locals: CampaignLocals,
    ): ImportPlan {
        val records = plannedRecords(header, file, locals)
        val sessions = plannedSessions(file, locals, records)
        val me = file.filterIsInstance<FileRecord.Party>().firstOrNull { it.isPlayerCharacter }
        return ImportPlan(
            mode = header.mode,
            records = records,
            sessions = sessions,
            personal = if (header.mode == ExportMode.SELF) PersonalCopy(me?.name, ownCopy(me, locals)) else null,
        )
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun plannedRecords(
        header: FileHeader,
        file: List<FileRecord>,
        locals: CampaignLocals,
    ): MutableList<PlannedRecord> {
        val records = mutableListOf<PlannedRecord>()

        fun add(
            kind: ExchangeKind,
            id: Long,
            originId: String?,
            name: String,
            diff: (Long) -> List<FieldChange>,
        ) {
            val ref = ExchangeRef(kind, id)
            records +=
                when (val match = locals.match(kind, originId, name)) {
                    is RecordMatch.ByOrigin ->
                        diff(
                            match.id,
                        ).withoutRenameIfAliased(locals, kind, match.id, originId).let {
                            PlannedRecord(
                                ref,
                                name,
                                if (it.isEmpty()) RecordStatus.SAME else RecordStatus.CHANGED,
                                changes = it,
                            )
                        }
                    is RecordMatch.ByName -> PlannedRecord(ref, name, RecordStatus.SAME_NAME, changes = diff(match.id))
                    is RecordMatch.Similar -> PlannedRecord(ref, name, RecordStatus.SIMILAR, similarTo = match.name)
                    RecordMatch.None ->
                        PlannedRecord(
                            ref,
                            name,
                            if (locals.isDeleted(kind, originId)) RecordStatus.DELETED_BEFORE else RecordStatus.NEW,
                        )
                }
        }
        file.forEach { record ->
            when (record) {
                is FileRecord.Species ->
                    add(ExchangeKind.SPECIES, record.id, record.originId, record.name) { id ->
                        diffSpecies(
                            record,
                            locals.species.first {
                                it.id ==
                                    id
                            },
                        )
                    }
                is FileRecord.Place ->
                    add(ExchangeKind.PLACE, record.id, record.originId, record.name) { id ->
                        diffPlace(
                            record,
                            locals.places.first {
                                it.id ==
                                    id
                            },
                        )
                    }
                is FileRecord.Npc ->
                    add(ExchangeKind.NPC, record.id, record.originId, record.name) { id ->
                        diffNpc(
                            record,
                            locals.npcs.first {
                                it.id ==
                                    id
                            },
                        )
                    }
                is FileRecord.Quest ->
                    add(ExchangeKind.QUEST, record.id, record.originId, record.title) { id ->
                        diffQuest(
                            record,
                            locals.quests.first {
                                it.id ==
                                    id
                            },
                        )
                    }
                is FileRecord.Party ->
                    add(ExchangeKind.PARTY, record.id, record.originId, record.name) { id ->
                        diffParty(
                            record,
                            locals.party.first {
                                it.id ==
                                    id
                            },
                            header.partyNotes,
                        )
                    }
                else -> Unit
            }
        }
        return records
    }

    private fun ownCopy(
        me: FileRecord.Party?,
        locals: CampaignLocals,
    ): Boolean {
        val mine = locals.party.firstOrNull { it.is_player_character } ?: return false
        val match = me?.let { locals.match(ExchangeKind.PARTY, it.originId, it.name) }
        return match is RecordMatch.ByOrigin && match.id == mine.id
    }

    // two records told to be the same may keep different names, that is no reason to ask again
    private fun List<FieldChange>.withoutRenameIfAliased(
        locals: CampaignLocals,
        kind: ExchangeKind,
        id: Long,
        fileOrigin: String?,
    ): List<FieldChange> =
        if (fileOrigin != null &&
            locals.originOf(kind, id) != fileOrigin
        ) {
            filter { it.field != RecordField.NAME }
        } else {
            this
        }

    private fun plannedSessions(
        file: List<FileRecord>,
        locals: CampaignLocals,
        records: MutableList<PlannedRecord>,
    ): List<PlannedSession> {
        val fileNotes = file.filterIsInstance<FileRecord.Entry>().groupBy { it.sessionId }
        return file.filterIsInstance<FileRecord.Session>().mapNotNull { session ->
            val twin = locals.sessionTwin(session.originId, session.title, session.date.date)
            val theirs = fileNotes[session.id].orEmpty().map { it.originId.orEmpty() }
            if (twin == null) {
                if (locals.isDeleted(ExchangeKind.SESSION, session.originId)) {
                    records +=
                        PlannedRecord(
                            ExchangeRef(ExchangeKind.SESSION, session.id),
                            session.title,
                            RecordStatus.DELETED_BEFORE,
                        )
                    return@mapNotNull null
                }
                PlannedSession(
                    session.id,
                    session.title,
                    session.date.date,
                    SessionStatus.NEW,
                    noteOverlap(emptyList(), theirs),
                )
            } else {
                val overlap = noteOverlap(locals.entries[twin.id].orEmpty().map { it.origin_id }, theirs)
                val status = if (overlap.theirs == overlap.common) SessionStatus.SAME else SessionStatus.MERGE
                PlannedSession(session.id, session.title, session.date.date, status, overlap)
            }
        }
    }
}
