package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository

/** the turn goes back, the party member whose turn it becomes again has the reaction back */
class CombatTurnBack(
    private val repository: CombatRepository,
) {
    /** the position it went back to, null when it is the start of the fight and nothing changed */
    suspend fun previous(
        combat: Combat,
        ownCharacterIds: Set<Long>,
    ): TurnPosition? {
        val ordered = combat.combatants.inTurnOrder()
        val position = previousTurn(ordered, TurnPosition(combat.round, combat.turn))
        if (position != null) {
            repository.setTurn(combat.id, position.round, position.turn)
            repository.refreshReaction(ordered, position, ownCharacterIds)
        }
        return position
    }
}
