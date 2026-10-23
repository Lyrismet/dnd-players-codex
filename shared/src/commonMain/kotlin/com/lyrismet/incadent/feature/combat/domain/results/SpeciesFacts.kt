package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase

/** the hit points past fights hint at: more than [low] since one survived that, at most [high] since one fell */
data class HitRange(
    val low: Int?,
    val high: Int?,
)

/** what the bestiary shows of the fights a species was in */
data class SpeciesCombatFacts(
    val fights: Int,
    val hitRange: HitRange?,
)

/** the saved fights a species was in and the hit points they hint at, a species no fight touched is left out */
fun speciesCombatFacts(
    combats: List<Combat>,
    observations: Map<Long, List<SpeciesHitObservation>>,
): Map<Long, SpeciesCombatFacts> {
    val fights =
        combats
            .filter { it.phase == CombatPhase.FINISHED }
            .flatMap { combat -> combat.combatants.mapNotNull { it.speciesId }.distinct() }
            .groupingBy { it }
            .eachCount()
    return (fights.keys + observations.keys).associateWith { id ->
        SpeciesCombatFacts(fights[id] ?: 0, observations[id].orEmpty().toRange())
    }
}

private fun List<SpeciesHitObservation>.toRange(): HitRange? {
    val low = filter { !it.died }.maxOfOrNull { it.damage }?.plus(1)
    val high = filter { it.died }.minOfOrNull { it.damage }
    return if (low == null && high == null) null else HitRange(low, high)
}
