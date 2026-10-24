package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

fun startReadiness(participants: List<Combatant>): StartReadiness {
    val missing = participants.count { it.turnOrder == null }
    return when {
        participants.none { it.side == CombatSide.ENEMY } -> StartReadiness.NeedEnemy
        missing > 0 -> StartReadiness.NeedOrder(missing)
        else -> StartReadiness.Ready(participants.size)
    }
}

/** the place a tap gives: none when it had one, otherwise the smallest number not taken yet */
fun toggledTurnOrder(
    participants: List<Combatant>,
    member: Combatant,
): Int? {
    if (member.turnOrder != null) return null
    val used = participants.mapNotNull { it.turnOrder }.toSet()
    return generateSequence(1) { it + 1 }.first { it !in used }
}

/** a reinforcement moves last in the round */
fun reinforcementOrder(participants: List<Combatant>): Int =
    (participants.mapNotNull { it.turnOrder }.maxOrNull() ?: 0) + 1

/** the three pips of death saves: pressing the filled last pip empties it, any other sets the count to it */
fun toggledSaves(
    current: Int,
    index: Int,
): Int = if (current == index + 1) index else index + 1
