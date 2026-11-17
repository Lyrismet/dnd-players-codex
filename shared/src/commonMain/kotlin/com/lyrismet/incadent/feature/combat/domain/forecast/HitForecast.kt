package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.math.roundToInt

/** a state marked on the master's words, with the damage and healing at that moment and the place on the scale */
data class StateMark(
    val state: CombatState,
    val damageAtMark: Int,
    val healedAtMark: Int = 0,
    val permille: Int? = null,
)

/** the hit points left as the player typed them, with the damage and healing the participant had at that moment */
data class ExactMark(
    val remaining: Int,
    val damageAtMark: Int,
    val healedAtMark: Int = 0,
)

/** what a past fight taught about the species: it died or survived after [damage] */
data class SpeciesHitObservation(
    val damage: Int,
    val died: Boolean,
)

data class HitForecastInput(
    val damageTaken: Int,
    val marks: List<StateMark> = emptyList(),
    val speciesObservations: List<SpeciesHitObservation> = emptyList(),
    val healed: Int = 0,
    val exact: ExactMark? = null,
)

/** where an estimate comes from, the scale the player moved or what earlier fights taught about the species */
enum class ForecastSource { SCALE, SPECIES }

sealed interface HitForecast {
    data object Unknown : HitForecast

    /** the participant has more than [damage] hits, the fact that it is still standing */
    data class AtLeast(
        val damage: Int,
    ) : HitForecast

    data class Estimate(
        val max: Int,
        val remaining: Int,
        val source: ForecastSource = ForecastSource.SCALE,
    ) : HitForecast

    /** typed by the player and moved on by the damage and healing told since */
    data class Exact(
        val remaining: Int,
    ) : HitForecast
}

private const val FULL_PERMILLE = 1000.0

// max hits per damage taken at the mark, 22 at bloodied is about 44 and 22 at barely standing is about 29
private val MARK_MAX_PER_DAMAGE =
    mapOf(
        CombatState.BLOODIED to 2.0,
        CombatState.BARELY_STANDING to 4.0 / 3.0,
    )

fun forecastHits(input: HitForecastInput): HitForecast {
    val exact = exactRemaining(input)
    return if (exact != null) HitForecast.Exact(exact) else estimateFromFacts(input)
}

private fun estimateFromFacts(input: HitForecastInput): HitForecast {
    val taken = netDamage(input.damageTaken, input.healed)
    val survivedAfter = input.speciesObservations.filter { !it.died }.maxOfOrNull { it.damage } ?: 0
    val floor = maxOf(taken, survivedAfter)
    val marked = markEstimate(input.marks)
    val estimate = marked ?: speciesEstimate(input.speciesObservations)
    val source = if (marked != null) ForecastSource.SCALE else ForecastSource.SPECIES
    return when {
        estimate != null && estimate > floor ->
            HitForecast.Estimate(
                estimate,
                (estimate - taken).coerceAtLeast(0),
                source,
            )
        floor > 0 -> HitForecast.AtLeast(floor)
        else -> HitForecast.Unknown
    }
}

private fun netDamage(
    damage: Int,
    healed: Int,
): Int = (damage - healed).coerceAtLeast(0)

private fun exactRemaining(input: HitForecastInput): Int? {
    val exact = input.exact
    return if (exact == null) {
        null
    } else {
        val moved = (input.damageTaken - exact.damageAtMark) - (input.healed - exact.healedAtMark)
        (exact.remaining - moved).coerceAtLeast(0)
    }
}

private fun markEstimate(marks: List<StateMark>): Int? {
    val mark =
        marks.lastOrNull {
            (it.permille != null || it.state in MARK_MAX_PER_DAMAGE) &&
                netDamage(it.damageAtMark, it.healedAtMark) > 0 &&
                maxPerDamage(it) != null
        }
    val factor = mark?.let(::maxPerDamage)
    return if (mark != null && factor != null) {
        (netDamage(mark.damageAtMark, mark.healedAtMark) * factor).roundToInt()
    } else {
        null
    }
}

/** how many times the damage taken at the mark the maximum is, none at the whole end of the scale */
private fun maxPerDamage(mark: StateMark): Double? =
    when {
        mark.permille == null -> MARK_MAX_PER_DAMAGE.getValue(mark.state)
        mark.permille >= FULL_PERMILLE -> null
        else -> 1.0 / (1.0 - mark.permille / FULL_PERMILLE)
    }

private fun speciesEstimate(observations: List<SpeciesHitObservation>): Int? =
    observations
        .filter { it.died }
        .map { it.damage }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()
