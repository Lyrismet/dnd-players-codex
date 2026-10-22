package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TurnOrderTest {
    private fun member(
        id: Long,
        side: CombatSide = CombatSide.ENEMY,
        order: Int? = null,
        state: CombatState = CombatState.WHOLE,
        stance: CombatStance = CombatStance.ENEMY,
        dead: Boolean = false,
    ) = Combatant(
        id = id,
        name = "М$id",
        side = side,
        npcId = null,
        speciesId = null,
        partyMemberId = null,
        enteredWhole = true,
        state = state,
        joinedRound = 1,
        deathSuccesses = 0,
        deathFailures = 0,
        turnOrder = order,
        stance = stance,
        dead = dead,
    )

    @Test
    fun `the order follows the places the master named`() {
        val list = listOf(member(1, order = 3), member(2, order = 1), member(3, order = 2))
        assertEquals(listOf(2L, 3L, 1L), list.inTurnOrder().map { it.id })
    }

    @Test
    fun `a member without a place comes after the placed ones`() {
        val list = listOf(member(1), member(2, order = 1))
        assertEquals(listOf(2L, 1L), list.inTurnOrder().map { it.id })
    }

    @Test
    fun `passing the last member starts a new round`() {
        val list = listOf(member(1), member(2))
        assertEquals(TurnPosition(round = 2, turn = 0), nextTurn(list, TurnPosition(1, 1)))
    }

    @Test
    fun `a fallen enemy is skipped`() {
        val list = listOf(member(1), member(2, state = CombatState.DOWN), member(3))
        assertEquals(TurnPosition(1, 2), nextTurn(list, TurnPosition(1, 0)))
    }

    @Test
    fun `a party member on the ground still gets a turn`() {
        val list = listOf(member(1), member(2, side = CombatSide.PARTY, state = CombatState.DOWN))
        assertEquals(TurnPosition(1, 1), nextTurn(list, TurnPosition(1, 0)))
    }

    @Test
    fun `a dead party member and a fled enemy are skipped`() {
        val list =
            listOf(member(1), member(2, side = CombatSide.PARTY, dead = true), member(3, stance = CombatStance.FLED))
        assertEquals(TurnPosition(2, 0), nextTurn(list, TurnPosition(1, 0)))
    }

    @Test
    fun `removing someone before the turn keeps the same one moving`() {
        assertEquals(1, turnAfterRemoval(turn = 2, removedIndex = 0, newSize = 3))
    }

    @Test
    fun `removing the last one while it moves wraps to the first`() {
        assertEquals(0, turnAfterRemoval(turn = 2, removedIndex = 2, newSize = 2))
    }

    @Test
    fun `hostile enemies do not count the fallen the fled and the friendly`() {
        val list =
            listOf(
                member(1),
                member(2, state = CombatState.DOWN),
                member(3, stance = CombatStance.NEUTRAL),
                member(4, stance = CombatStance.FLED),
                member(5, side = CombatSide.PARTY),
            )
        assertEquals(1, list.hostileCount())
    }

    @Test
    fun `an unfinished fight reads as interrupted`() {
        assertEquals(CombatOutcome.INTERRUPTED, autoOutcome(listOf(member(1))))
    }

    @Test
    fun `no hostile left reads as a victory`() {
        assertEquals(CombatOutcome.VICTORY, autoOutcome(listOf(member(1, state = CombatState.DOWN))))
    }

    @Test
    fun `enemies who turned friendly with none fallen read as a peace`() {
        assertEquals(CombatOutcome.PEACE, autoOutcome(listOf(member(1, stance = CombatStance.FRIEND))))
    }

    @Test
    fun `a fight needs an enemy first`() {
        assertEquals(StartReadiness.NeedEnemy, startReadiness(listOf(member(1, side = CombatSide.PARTY))))
    }

    @Test
    fun `a fight needs every place in the order`() {
        val list = listOf(member(1, order = 1), member(2), member(3))
        assertEquals(StartReadiness.NeedOrder(left = 2), startReadiness(list))
    }

    @Test
    fun `a fight is ready when all have a place`() {
        assertEquals(
            StartReadiness.Ready(count = 2),
            startReadiness(listOf(member(1, order = 1), member(2, order = 2))),
        )
    }

    @Test
    fun `a tap gives the smallest free number`() {
        val list = listOf(member(1, order = 1), member(2, order = 3), member(3))
        assertEquals(2, toggledTurnOrder(list, list[2]))
    }

    @Test
    fun `a second tap takes the number back`() {
        val list = listOf(member(1, order = 1))
        assertNull(toggledTurnOrder(list, list[0]))
    }

    @Test
    fun `a reinforcement moves after the last one`() {
        assertEquals(4, reinforcementOrder(listOf(member(1, order = 1), member(2, order = 3))))
    }

    @Test
    fun `a death save pip sets the count to itself`() {
        assertEquals(2, toggledSaves(current = 0, index = 1))
    }

    @Test
    fun `pressing the last filled pip empties it`() {
        assertEquals(1, toggledSaves(current = 2, index = 1))
    }
}
