package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.grouping.with
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
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
    // the shields and facts the fight kept on the participant, they moved to the new record and come back with the undo
    val oldShields: Map<DamageType, DamageState> = emptyMap(),
    val oldFacts: List<String> = emptyList(),
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
                combats.setLabel(member.id, choice.name, true)
                null
            }
            is NameChoice.Existing -> {
                combats.linkNpc(member.id, choice.npcId, choice.name)
                PromotionTrace(member.id, choice.npcId, createdNpc = false, member.name, member.npcId)
            }
            is NameChoice.New -> {
                val speciesRecord = member.speciesId?.let { species.getById(it) }
                val role = RoleParts.ofStored(member.role)
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
                            characterClass = role.characterClass,
                            subclass = role.subclass,
                            vulnerabilities = member.vulnerabilities,
                            facts = member.facts,
                        ),
                    )
                combats.linkNpc(member.id, id, choice.name)
                combats.setOwnShields(member.id, emptyMap())
                combats.setOwnFacts(member.id, emptyList())
                PromotionTrace(
                    member.id,
                    id,
                    createdNpc = true,
                    member.name,
                    member.npcId,
                    member.vulnerabilities,
                    member.facts,
                )
            }
        }

    /** takes the naming back: the participant is what it was and an npc made for it is gone */
    suspend fun undo(trace: PromotionTrace) {
        combats.linkNpc(trace.memberId, trace.oldNpcId, trace.oldName)
        if (!trace.createdNpc) return
        npcs.delete(trace.npcId)
        combats.setOwnShields(trace.memberId, trace.oldShields)
        combats.setOwnFacts(trace.memberId, trace.oldFacts)
    }

    /** moves what the fight learned about [types] from the species card to the npc card, returns the way back */
    suspend fun moveLearned(
        combatId: Long,
        trace: PromotionTrace,
        learned: List<LearnedVulnerability>,
        types: Set<DamageType>,
    ): suspend () -> Unit {
        val moved = mutableListOf<MovedVulnerability>()
        learned.filter { it.type in types }.forEach { item ->
            val speciesRecord = species.getById(item.speciesId) ?: return@forEach
            val npc = npcs.getById(trace.npcId) ?: return@forEach
            val now = speciesRecord.vulnerabilities[item.type] ?: return@forEach
            npcs.upsert(npc.copy(vulnerabilities = npc.vulnerabilities.with(item.type, now)))
            species.upsert(
                speciesRecord.copy(vulnerabilities = speciesRecord.vulnerabilities.with(item.type, item.previous)),
            )
            combats.removeLearned(combatId, item.speciesId, item.type)
            moved += MovedVulnerability(item, npc.vulnerabilities[item.type], now)
        }
        return {
            moved.forEach { (item, npcBefore, speciesNow) ->
                species.getById(item.speciesId)?.let {
                    species.upsert(it.copy(vulnerabilities = it.vulnerabilities.with(item.type, speciesNow)))
                }
                npcs.getById(trace.npcId)?.let {
                    val restored = it.vulnerabilities.with(item.type, npcBefore)
                    npcs.upsert(it.copy(vulnerabilities = restored))
                }
                combats.addLearned(combatId, item)
            }
        }
    }
}

private data class MovedVulnerability(
    val item: LearnedVulnerability,
    val npcBefore: DamageState?,
    val speciesNow: DamageState,
)

private fun CombatStance.toNpcStatus() =
    when (this) {
        CombatStance.NEUTRAL -> NpcStatus.NEUTRAL
        CombatStance.FRIEND -> NpcStatus.FRIEND
        CombatStance.ENEMY, CombatStance.FLED -> NpcStatus.ENEMY
    }
