package com.lyrismet.incadent.feature.combat.domain.round

const val SECONDS_PER_ROUND = 6
const val SECONDS_IN_MINUTE = 60

/** the time in the fiction: minutes and the seconds left over */
data class GameTime(
    val minutes: Int,
    val seconds: Int,
)

/** a round is six seconds and the first one starts at zero, so the clock moves only when the round changes */
fun gameTime(round: Int): GameTime {
    val total = (round - 1).coerceAtLeast(0) * SECONDS_PER_ROUND
    return GameTime(total / SECONDS_IN_MINUTE, total % SECONDS_IN_MINUTE)
}
