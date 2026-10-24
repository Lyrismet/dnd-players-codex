package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.math.roundToInt

/** a state the player marked on the master's words, with the damage the participant had taken at that moment */
data class StateMark(
    val state: CombatState,
    val damageAtMark: Int,
)

/** what a past fight taught about the species: it died or survived after [damage] */
data class SpeciesHitObservation(
    val damage: Int,
    val died: Boolean,
)

data class HitForecastInput(
    val damageTaken: Int,
    val enteredWhole: Boolean = true,
    val marks: List<StateMark> = emptyList(),
    val speciesObservations: List<SpeciesHitObservation> = emptyList(),
)

sealed interface HitForecast {
    data object Unknown : HitForecast

    /** the participant has more than [damage] hits, the fact that it is still standing */
    data class AtLeast(
        val damage: Int,
    ) : HitForecast

    data class Estimate(
        val max: Int,
        val remaining: Int,
    ) : HitForecast
}

// max hits per damage taken at the mark, 22 at bloodied is about 44 and 22 at barely standing is about 29
private val MARK_MAX_PER_DAMAGE =
    mapOf(
        CombatState.BLOODIED to 2.0,
        CombatState.BARELY_STANDING to 4.0 / 3.0,
    )

fun forecastHits(input: HitForecastInput): HitForecast {
    if (!input.enteredWhole) return HitForecast.Unknown
    val taken = input.damageTaken
    val survivedAfter = input.speciesObservations.filter { !it.died }.maxOfOrNull { it.damage } ?: 0
    val floor = maxOf(taken, survivedAfter)
    val estimate = markEstimate(input.marks) ?: speciesEstimate(input.speciesObservations)
    return when {
        estimate != null && estimate > floor -> HitForecast.Estimate(estimate, (estimate - taken).coerceAtLeast(0))
        floor > 0 -> HitForecast.AtLeast(floor)
        else -> HitForecast.Unknown
    }
}

private fun markEstimate(marks: List<StateMark>): Int? =
    marks
        .lastOrNull { it.state in MARK_MAX_PER_DAMAGE && it.damageAtMark > 0 }
        ?.let { (it.damageAtMark * MARK_MAX_PER_DAMAGE.getValue(it.state)).roundToInt() }

private fun speciesEstimate(observations: List<SpeciesHitObservation>): Int? =
    observations
        .filter { it.died }
        .map { it.damage }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()
