package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

private enum class Camp { ME, PARTY, ALLY, ENEMY }

private fun Combatant.camp(ownIds: Set<Long>): Camp =
    when {
        id in ownIds -> Camp.ME
        side == CombatSide.PARTY -> Camp.PARTY
        side == CombatSide.ALLY -> Camp.ALLY
        else -> Camp.ENEMY
    }

private fun damageOrder(actor: Camp): List<Camp> =
    if (actor == Camp.ENEMY) {
        listOf(Camp.ME, Camp.PARTY, Camp.ALLY, Camp.ENEMY)
    } else {
        listOf(Camp.ENEMY, Camp.ALLY, Camp.ME, Camp.PARTY)
    }

private fun healOrder(actor: Camp): List<Camp> =
    when (actor) {
        Camp.ENEMY -> listOf(Camp.ENEMY, Camp.ME, Camp.PARTY, Camp.ALLY)
        Camp.ALLY -> listOf(Camp.ALLY, Camp.ME, Camp.PARTY, Camp.ENEMY)
        else -> listOf(Camp.ME, Camp.PARTY, Camp.ALLY, Camp.ENEMY)
    }

/** targets in the order the calculator offers them, the turn order holds inside a group and without an actor */
internal fun List<Combatant>.inTargetOrder(
    actor: Combatant?,
    mode: CalcMode,
    ownIds: Set<Long>,
): List<Combatant> {
    if (actor == null) return this
    val order = actor.camp(ownIds).let { if (mode == CalcMode.DAMAGE) damageOrder(it) else healOrder(it) }
    // healing starts with the one who heals
    return sortedBy { if (mode != CalcMode.DAMAGE && it.id == actor.id) -1 else order.indexOf(it.camp(ownIds)) }
}

// damage comes from the chosen source, healing from the one whose turn it is
internal fun actorOf(
    draft: CalcDraft,
    ctx: CalcContext,
): Combatant? = if (draft.mode == CalcMode.DAMAGE) ctx.ordered.firstOrNull { it.id == draft.sourceId } else ctx.mover
