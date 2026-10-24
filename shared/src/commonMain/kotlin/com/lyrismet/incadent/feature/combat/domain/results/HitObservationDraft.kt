package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** what one enemy of a known species taught: it fell after [damage] or was still up after [damage] */
data class HitObservationDraft(
    val memberId: Long,
    val memberName: String,
    val speciesId: Long,
    val damage: Int,
    val fell: Boolean,
)

/** the damage a creature had taken when it first went down, none if it stayed up, and the damage in the end */
private class Course(
    val fellAt: Int?,
    val taken: Int,
)

private fun courseOf(
    combat: Combat,
    member: Combatant,
): Course {
    var taken = 0
    var fellAt: Int? = null
    combat.events.filter { it.targetId == member.id }.forEach { event ->
        when (event.kind) {
            CombatEventKind.DAMAGE -> taken += event.amount
            CombatEventKind.HEAL -> taken = (taken - event.amount).coerceAtLeast(0)
            CombatEventKind.DOWN -> if (fellAt == null) fellAt = taken
            else -> Unit
        }
    }
    return Course(fellAt, taken)
}

/** only enemies that entered whole count, a creature that came wounded says nothing about its full hit points */
fun hitObservations(combat: Combat): List<HitObservationDraft> =
    combat.combatants
        .filter { it.side == CombatSide.ENEMY && it.enteredWhole }
        .mapNotNull { member -> member.speciesId?.let { observationOf(combat, member, it) } }

private fun observationOf(
    combat: Combat,
    member: Combatant,
    speciesId: Long,
): HitObservationDraft? {
    val course = courseOf(combat, member)
    val fell = course.fellAt
    return when {
        fell != null && fell > 0 -> HitObservationDraft(member.id, member.name, speciesId, fell, true)
        fell == null && course.taken > 0 && !member.captured ->
            HitObservationDraft(member.id, member.name, speciesId, course.taken, false)
        else -> null
    }
}
