package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.round.SECONDS_IN_MINUTE
import com.lyrismet.incadent.feature.combat.domain.round.SECONDS_PER_ROUND

/** how long the fight lasted in the fiction: exact seconds under a minute, about so many minutes after it */
sealed interface FightDuration {
    data class Seconds(
        val value: Int,
    ) : FightDuration

    data class AboutMinutes(
        val value: Int,
    ) : FightDuration
}

/** whole rounds only, a fight that reached round four played four of them, 24 seconds */
fun fightDuration(rounds: Int): FightDuration {
    val seconds = rounds.coerceAtLeast(0) * SECONDS_PER_ROUND
    return if (seconds < SECONDS_IN_MINUTE) {
        FightDuration.Seconds(seconds)
    } else {
        FightDuration.AboutMinutes((seconds + SECONDS_IN_MINUTE / 2) / SECONDS_IN_MINUTE)
    }
}
