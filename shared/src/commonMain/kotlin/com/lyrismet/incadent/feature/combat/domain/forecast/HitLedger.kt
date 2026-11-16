package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind

/** what the events of one participant add up to, read once in the order they were told */
data class HitLedger(
    val damage: Int,
    val healed: Int,
    val marks: List<StateMark>,
    val exact: ExactMark?,
    val healedSinceSet: Int,
) {
    val net: Int get() = (damage - healed).coerceAtLeast(0)
}

fun Combat.ledgerOf(memberId: Long): HitLedger {
    var damage = 0
    var healed = 0
    var healedSinceSet = 0
    var exact: ExactMark? = null
    val marks = mutableListOf<StateMark>()
    events.forEach { event ->
        if (event.targetId != memberId) return@forEach
        when (event.kind) {
            CombatEventKind.DAMAGE -> damage += event.amount
            CombatEventKind.HEAL -> {
                healed += event.amount
                healedSinceSet += event.amount
            }
            CombatEventKind.STATE -> {
                event.state?.let { marks += StateMark(it, event.amount, healed, event.raw.takeIf { raw -> raw > 0 }) }
                // a later word or scale mark says something newer than the typed hit points
                exact = null
            }
            CombatEventKind.SET -> {
                exact = ExactMark(event.amount, damage, healed)
                healedSinceSet = 0
            }
            else -> Unit
        }
    }
    return HitLedger(damage, healed, marks, exact, healedSinceSet)
}
