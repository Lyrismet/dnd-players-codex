package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.mention.MentionBody
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.MentionHits
import com.lyrismet.incadent.core.mention.MentionIndex
import com.lyrismet.incadent.core.mention.countMentionHits
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository

/** every record the codex lists plus the note texts and mention candidates a delete is checked against */
internal class CodexRecords(
    val parties: List<PartyMember>,
    val npcs: List<Npc>,
    val quests: List<Quest>,
    val locations: List<Location>,
    val entries: List<SessionEntry> = emptyList(),
    val mentionIndex: MentionIndex = MentionIndex.EMPTY,
)

/** what a delete does, what the undo toast says, and how to put the entity back */
internal class CodexDeletionPlan(
    val subtitle: String,
    val delete: suspend () -> Unit,
    val restore: suspend () -> Unit,
    // the notes that still mention the record - above zero the delete asks first and leaves "?" tags behind
    val mentionHits: MentionHits = MentionHits(notes = 0, sessions = 0),
    // the name the record is mentioned by and what its title is called in the warning
    val name: String = "",
)

/** builds the delete/restore pair for one codex entity - restoring also re-links whatever referenced it */
internal class CodexDeletionPlanner(
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) {
    /** the delete/restore of [ref] - undo puts the record back under its old id, so tokens in notes find it */
    fun planFor(
        ref: EntityRef,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? {
        val base = basePlan(ref, records, labels)
        val mentioned = mentionedAs(ref, records)
        return if (base == null || mentioned == null) base else withMentions(base, mentioned, records)
    }

    private fun withMentions(
        base: CodexDeletionPlan,
        mentioned: MentionedRecord,
        records: CodexRecords,
    ): CodexDeletionPlan {
        val bodies = records.entries.map { MentionBody(it.sessionNoteId, it.body) }
        val hits = countMentionHits(mentioned.entity, bodies, records.mentionIndex)
        return CodexDeletionPlan(
            subtitle = base.subtitle,
            delete = base.delete,
            restore = base.restore,
            mentionHits = hits,
            name = mentioned.entity.name,
        )
    }

    private fun basePlan(
        ref: EntityRef,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        when (ref) {
            is EntityRef.Party -> partyPlan(ref.id, records, labels)
            is EntityRef.Npc -> npcPlan(ref.id, records, labels)
            is EntityRef.Quest -> questPlan(ref.id, records, labels)
            is EntityRef.Location -> locationPlan(ref.id, records, labels)
        }

    private fun partyPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.parties.find { it.id == id }?.let { member ->
            // the list row carries only the thumbnail, so the stored record with its photo is read before the delete
            var stored: PartyMember? = null
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.PARTY)} · ${member.name}",
                delete = {
                    stored = partyRepository.getById(id)
                    partyRepository.delete(id)
                },
                restore = { partyRepository.upsert(stored ?: member) },
            )
        }

    private fun npcPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.npcs.find { it.id == id }?.let { npc ->
            val givenQuestIds = records.quests.filter { it.givenByNpcId == id }.map { it.id }
            var stored: Npc? = null
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.NPC)} · ${npc.name}",
                delete = {
                    stored = npcRepository.getById(id)
                    npcRepository.delete(id)
                },
                restore = {
                    val newId = npcRepository.upsert(stored ?: npc)
                    givenQuestIds.forEach { questId ->
                        questRepository.getById(questId)?.let {
                            questRepository.upsert(it.copy(givenByNpcId = newId))
                        }
                    }
                },
            )
        }

    private fun questPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.quests.find { it.id == id }?.let { quest ->
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.QUEST)} · ${quest.title}",
                delete = { questRepository.delete(id) },
                restore = { questRepository.upsert(quest) },
            )
        }

    private fun locationPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.locations.find { it.id == id }?.let { location ->
            val npcIdsHere = records.npcs.filter { id in it.locationIds }.map { it.id }
            val childIds = records.locations.filter { it.parentId == id }.map { it.id }
            val questIdsHere = records.quests.filter { it.locationId == id }.map { it.id }
            var stored: Location? = null
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.LOCATION)} · ${location.name}",
                delete = {
                    stored = locationRepository.getById(id)
                    locationRepository.delete(id)
                },
                restore = {
                    val newId = locationRepository.upsert(stored ?: location)
                    npcIdsHere.forEach { npcId ->
                        npcRepository.getById(npcId)?.let {
                            npcRepository.upsert(it.copy(locationIds = (it.locationIds + newId).distinct()))
                        }
                    }
                    questIdsHere.forEach { questId ->
                        questRepository.getById(questId)?.let {
                            questRepository.upsert(it.copy(locationId = newId))
                        }
                    }
                    childIds.forEach { childId ->
                        locationRepository.getById(childId)?.let {
                            locationRepository.upsert(it.copy(parentId = newId))
                        }
                    }
                },
            )
        }
}

private class MentionedRecord(
    val entity: MentionEntity,
    val type: DeletedMentionType,
)

private fun mentionedAs(
    ref: EntityRef,
    records: CodexRecords,
): MentionedRecord? =
    when (ref) {
        is EntityRef.Party ->
            records.parties.find { it.id == ref.id }?.let {
                MentionedRecord(MentionEntity.PartyMention(it.id, it.name), DeletedMentionType.PARTY)
            }
        is EntityRef.Npc ->
            records.npcs.find { it.id == ref.id }?.let {
                MentionedRecord(
                    MentionEntity.NpcMention(it.id, it.name, it.status, it.lifeState),
                    DeletedMentionType.NPC,
                )
            }
        is EntityRef.Quest ->
            records.quests.find { it.id == ref.id }?.let {
                MentionedRecord(MentionEntity.QuestMention(it.id, it.title, it.status), DeletedMentionType.QUEST)
            }
        is EntityRef.Location ->
            records.locations.find { it.id == ref.id }?.let {
                MentionedRecord(MentionEntity.LocationMention(it.id, it.name), DeletedMentionType.LOCATION)
            }
    }
