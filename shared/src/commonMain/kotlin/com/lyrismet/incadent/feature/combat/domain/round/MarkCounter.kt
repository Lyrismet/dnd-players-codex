package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatMark
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** rounds a counted mark has left in [round], never below zero, none for a mark without a count */
fun CombatMark.remaining(round: Int): Int? = roundsLeft?.let { (it - (round - startedRound)).coerceAtLeast(0) }

/** the counter is on its last round */
fun CombatMark.isLastRound(round: Int): Boolean = remaining(round) == 1

fun CombatMark.isExpired(round: Int): Boolean = remaining(round) == 0

/** the marks whose time ran out when the fight went from [from] to [to], told once on the round they run out */
fun List<Combatant>.expiredBetween(
    from: Int,
    to: Int,
): List<CombatMark> =
    if (to <= from) {
        emptyList()
    } else {
        flatMap { it.marks }.filter { !it.isExpired(from) && it.isExpired(to) }
    }
