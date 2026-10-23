package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.grouping.with
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository

/** what a participant was before it was named, enough to take the naming back */
data class PromotionTrace(
    val memberId: Long,
    val npcId: Long,
    val createdNpc: Boolean,
    val oldName: String,
    val oldNpcId: Long?,
)

/** turns a participant made of a species into a named npc of the codex, in the middle of a fight */
class NpcPromotion(
    private val combats: CombatRepository,
    private val npcs: NpcRepository,
    private val species: SpeciesRepository,
) {
    /** makes the npc of [choice], or finds the known one, and links the participant to it */
    suspend fun promote(
        member: Combatant,
        choice: NameChoice,
    ): PromotionTrace? =
        when (choice) {
            is NameChoice.Plain -> {
                combats.renameCombatant(member.id, choice.name)
                null
            }
            is NameChoice.Existing -> {
                combats.linkNpc(member.id, choice.npcId, choice.name)
                PromotionTrace(member.id, choice.npcId, createdNpc = false, member.name, member.npcId)
            }
            is NameChoice.New -> {
                val speciesRecord = member.speciesId?.let { species.getById(it) }
                val id =
                    npcs.upsert(
                        Npc(
                            id = 0,
                            name = choice.name,
                            status = member.stance.toNpcStatus(),
                            lifeState = NpcLifeState.ALIVE,
                            description = member.note.trim(),
                            locationIds = emptyList(),
                            race = speciesRecord?.name.orEmpty(),
                            speciesId = member.speciesId,
                        ),
                    )
                combats.linkNpc(member.id, id, choice.name)
                PromotionTrace(member.id, id, createdNpc = true, member.name, member.npcId)
            }
        }

    /** takes the naming back: the participant is what it was and an npc made for it is gone */
    suspend fun undo(trace: PromotionTrace) {
        combats.linkNpc(trace.memberId, trace.oldNpcId, trace.oldName)
        if (trace.createdNpc) npcs.delete(trace.npcId)
    }

    /**
     * moves what the fight learned about [types] from the species card to the npc card and returns the way back
     */
    suspend fun moveLearned(
        combatId: Long,
        trace: PromotionTrace,
        learned: List<LearnedVulnerability>,
        types: Set<DamageType>,
    ): suspend () -> Unit {
        val moving = learned.filter { it.type in types }
        val npc = npcs.getById(trace.npcId)
        val speciesRecord = moving.firstOrNull()?.let { species.getById(it.speciesId) }
        if (npc == null || speciesRecord == null) return {}
        var npcMap = npc.vulnerabilities
        var speciesMap = speciesRecord.vulnerabilities
        moving.forEach { item ->
            val now = speciesMap[item.type] ?: return@forEach
            npcMap = npcMap.with(item.type, now)
            speciesMap = speciesMap.with(item.type, item.previous)
            combats.removeLearned(combatId, item.speciesId, item.type)
        }
        npcs.upsert(npc.copy(vulnerabilities = npcMap))
        species.upsert(speciesRecord.copy(vulnerabilities = speciesMap))
        return {
            species
                .getById(
                    speciesRecord.id,
                )?.let { species.upsert(it.copy(vulnerabilities = speciesRecord.vulnerabilities)) }
            npcs.getById(npc.id)?.let { npcs.upsert(it.copy(vulnerabilities = npc.vulnerabilities)) }
            moving.forEach { combats.addLearned(combatId, it) }
        }
    }
}

private fun CombatStance.toNpcStatus() =
    when (this) {
        CombatStance.NEUTRAL -> NpcStatus.NEUTRAL
        CombatStance.FRIEND -> NpcStatus.FRIEND
        CombatStance.ENEMY, CombatStance.FLED -> NpcStatus.ENEMY
    }
