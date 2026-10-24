package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.grouping.EnemyChip
import com.lyrismet.incadent.feature.combat.domain.grouping.enemyChips
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase

/** how a fight ended for the one who fell in it: the session it was saved to and who stood against them */
data class Fall(
    val sessionNoteId: Long?,
    val foes: List<EnemyChip>,
)

/** the chronicle of a party member: the sessions and fights they were in and the damage they dealt */
data class PartyCombatFacts(
    val sessions: Int,
    val fights: Int,
    val damage: Int,
)

/** what the saved fights say, for the codex to show: by species id, by party member id and by fight id */
data class CombatFacts(
    val species: Map<Long, SpeciesCombatFacts>,
    val party: Map<Long, PartyCombatFacts>,
    val falls: Map<Long, Fall>,
) {
    companion object {
        val EMPTY = CombatFacts(emptyMap(), emptyMap(), emptyMap())
    }
}

fun combatFacts(
    combats: List<Combat>,
    observations: Map<Long, List<SpeciesHitObservation>>,
): CombatFacts {
    val finished = combats.filter { it.phase == CombatPhase.FINISHED }
    return CombatFacts(
        species = speciesCombatFacts(combats, observations),
        party = partyCombatFacts(finished),
        falls = finished.associate { it.id to Fall(it.sessionNoteId, it.combatants.enemyChips()) },
    )
}

private fun partyCombatFacts(finished: List<Combat>): Map<Long, PartyCombatFacts> {
    val rows =
        finished.flatMap { combat ->
            combat.combatants.mapNotNull { member ->
                member.partyMemberId?.let { id ->
                    val dealt =
                        combat.events
                            .filter { it.kind == CombatEventKind.DAMAGE && it.sourceId == member.id }
                            .sumOf { it.amount }
                    Triple(id, combat.sessionNoteId, dealt)
                }
            }
        }
    return rows.groupBy { it.first }.mapValues { (_, list) ->
        PartyCombatFacts(list.mapNotNull { it.second }.distinct().size, list.size, list.sumOf { it.third })
    }
}
