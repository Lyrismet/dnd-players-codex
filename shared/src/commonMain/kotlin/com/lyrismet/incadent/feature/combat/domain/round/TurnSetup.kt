package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

fun startReadiness(participants: List<Combatant>): StartReadiness =
    if (participants.none { it.side == CombatSide.ENEMY }) {
        StartReadiness.NeedEnemy
    } else {
        StartReadiness.Ready(participants.size)
    }

/** the place a tap gives: none when it had one, otherwise the one after the last named, so the taps keep their order */
fun toggledTurnOrder(
    participants: List<Combatant>,
    member: Combatant,
): Int? = if (member.turnOrder != null) null else nextPlace(participants)

/** a reinforcement moves last in the round, behind the ones without a place it joins them as one more */
fun reinforcementOrder(participants: List<Combatant>): Int? =
    if (participants.any { it.turnOrder == null }) null else nextPlace(participants)

private fun nextPlace(participants: List<Combatant>): Int =
    (participants.mapNotNull { it.turnOrder }.maxOrNull() ?: 0) + 1

/** the three pips of death saves: pressing the filled last pip empties it, any other sets the count to it */
fun toggledSaves(
    current: Int,
    index: Int,
): Int = if (current == index + 1) index else index + 1
