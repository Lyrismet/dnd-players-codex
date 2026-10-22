package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** the participants as the master named the order, those without a place come after them */
fun List<Combatant>.inTurnOrder(): List<Combatant> =
    sortedWith(compareBy<Combatant> { it.turnOrder ?: Int.MAX_VALUE }.thenBy { it.id })

fun Combatant.isDown(): Boolean = state == CombatState.DOWN

/** a party member on the ground who has not died is rolling death saves */
fun Combatant.isDying(): Boolean = side == CombatSide.PARTY && isDown() && !dead

/** out of the fight: an enemy or an ally that went down or ran, or a party member who died */
fun Combatant.isOut(): Boolean = dead || (side != CombatSide.PARTY && (isDown() || stance == CombatStance.FLED))

/** the hit points are numbers only when they were given, otherwise they are guessed from the master's words */
fun Combatant.isTracked(): Boolean = hpMax != null

/** the enemies still standing and still enemies, a neutral or a fled one no longer counts */
fun List<Combatant>.hostileCount(): Int =
    count {
        it.side == CombatSide.ENEMY &&
            it.stance == CombatStance.ENEMY &&
            !it.isDown()
    }

/** the next one to move, those who are out are skipped and passing the last position starts a new round */
fun nextTurn(
    participants: List<Combatant>,
    from: TurnPosition,
): TurnPosition {
    val size = participants.size
    var position = from
    if (size > 0) {
        var found = false
        var step = 0
        while (!found && step < size) {
            val turn = (position.turn + 1) % size
            position = TurnPosition(if (turn == 0) position.round + 1 else position.round, turn)
            found = !participants[turn].isOut()
            step++
        }
    }
    return position
}

/** the turn position after the participant at [removedIndex] left, the one who moves keeps moving */
fun turnAfterRemoval(
    turn: Int,
    removedIndex: Int,
    newSize: Int,
): Int {
    val shifted = if (removedIndex < turn) turn - 1 else turn
    return if (shifted >= newSize) 0 else shifted
}

/** how the fight reads when it is ended without a choice: nobody hostile left is a win, or a peace if they gave up */
fun autoOutcome(participants: List<Combatant>): CombatOutcome {
    val foes = participants.filter { it.side == CombatSide.ENEMY }
    return when {
        participants.hostileCount() > 0 -> CombatOutcome.INTERRUPTED
        foes.any {
            it.stance == CombatStance.NEUTRAL || it.stance == CombatStance.FRIEND
        } &&
            foes.none { it.isDown() } ->
            CombatOutcome.PEACE
        else -> CombatOutcome.VICTORY
    }
}
