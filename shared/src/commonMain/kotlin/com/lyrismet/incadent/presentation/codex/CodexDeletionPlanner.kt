package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository

/** every record the codex lists, bundled so the planner takes one param instead of four */
internal class CodexRecords(
    val parties: List<PartyMember>,
    val npcs: List<Npc>,
    val quests: List<Quest>,
    val locations: List<Location>,
)

/** what a delete does, what the undo toast says, and how to put the entity back */
internal class CodexDeletionPlan(
    val subtitle: String,
    val delete: suspend () -> Unit,
    val restore: suspend () -> Unit,
)

/** builds the delete/restore pair for one codex entity - restoring also re-links whatever referenced it */
internal class CodexDeletionPlanner(
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) {
    fun planFor(
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
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.PARTY)} · ${member.name}",
                delete = { partyRepository.delete(id) },
                restore = { partyRepository.upsert(member.copy(id = 0)) },
            )
        }

    private fun npcPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.npcs.find { it.id == id }?.let { npc ->
            val givenQuestIds = records.quests.filter { it.givenByNpcId == id }.map { it.id }
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.NPC)} · ${npc.name}",
                delete = { npcRepository.delete(id) },
                restore = {
                    val newId = npcRepository.upsert(npc.copy(id = 0))
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
                restore = { questRepository.upsert(quest.copy(id = 0)) },
            )
        }

    private fun locationPlan(
        id: Long,
        records: CodexRecords,
        labels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        records.locations.find { it.id == id }?.let { location ->
            val npcIdsHere = records.npcs.filter { it.locationId == id }.map { it.id }
            val questIdsHere = records.quests.filter { it.locationId == id }.map { it.id }
            CodexDeletionPlan(
                subtitle = "${labels.getValue(CodexEntryType.LOCATION)} · ${location.name}",
                delete = { locationRepository.delete(id) },
                restore = {
                    val newId = locationRepository.upsert(location.copy(id = 0))
                    npcIdsHere.forEach { npcId ->
                        npcRepository.getById(npcId)?.let { npcRepository.upsert(it.copy(locationId = newId)) }
                    }
                    questIdsHere.forEach { questId ->
                        questRepository.getById(questId)?.let {
                            questRepository.upsert(it.copy(locationId = newId))
                        }
                    }
                },
            )
        }
}
