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
    fun `going back inside a round steps to the one before`() {
        val list = listOf(member(1), member(2), member(3))
        assertEquals(TurnPosition(1, 1), previousTurn(list, TurnPosition(1, 2)))
    }

    @Test
    fun `going back from the first turn of a round returns to the round before`() {
        val list = listOf(member(1), member(2))
        assertEquals(TurnPosition(2, 1), previousTurn(list, TurnPosition(3, 0)))
    }

    @Test
    fun `going back on the first turn of the fight is not possible`() {
        val list = listOf(member(1), member(2))
        assertNull(previousTurn(list, TurnPosition(1, 0)))
    }

    @Test
    fun `going back skips those who are out`() {
        val list = listOf(member(1), member(2, state = CombatState.DOWN), member(3))
        assertEquals(TurnPosition(1, 0), previousTurn(list, TurnPosition(1, 2)))
    }

    @Test
    fun `going back does not step before the first one who still plays`() {
        val list = listOf(member(1, state = CombatState.DOWN), member(2))
        assertNull(previousTurn(list, TurnPosition(1, 1)))
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
    fun `a fight starts with an incomplete order`() {
        val list = listOf(member(1, order = 1), member(2), member(3))
        assertEquals(StartReadiness.Ready(count = 3), startReadiness(list))
    }

    @Test
    fun `a fight starts with an empty order`() {
        assertEquals(StartReadiness.Ready(count = 2), startReadiness(listOf(member(1), member(2))))
    }

    @Test
    fun `a fight is ready when all have a place`() {
        assertEquals(
            StartReadiness.Ready(count = 2),
            startReadiness(listOf(member(1, order = 1), member(2, order = 2))),
        )
    }

    @Test
    fun `a tap gives the place after the last named one`() {
        val list = listOf(member(1, order = 1), member(2, order = 3), member(3))
        assertEquals(4, toggledTurnOrder(list, list[2]))
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
    fun `a reinforcement joins the unplaced ones when someone has no place`() {
        assertNull(reinforcementOrder(listOf(member(1, order = 1), member(2))))
    }

    @Test
    fun `the slots are dense with the named first and the rest as pending`() {
        val slots = listOf(member(1), member(2, order = 5), member(3, order = 9), member(4)).queueSlots()
        assertEquals(QueueSlot.Pending(3), slots[1L])
        assertEquals(QueueSlot.Named(1), slots[2L])
        assertEquals(QueueSlot.Named(2), slots[3L])
        assertEquals(QueueSlot.Pending(4), slots[4L])
    }

    @Test
    fun `nobody named makes every slot pending in the order they were added`() {
        val slots = listOf(member(2), member(1)).queueSlots()
        assertEquals(QueueSlot.Pending(1), slots[1L])
        assertEquals(QueueSlot.Pending(2), slots[2L])
    }

    @Test
    fun `the queue status tells none some or all named`() {
        assertEquals(QueueStatus.Unnamed, listOf(member(1), member(2)).queueStatus())
        assertEquals(QueueStatus.Partial(1), listOf(member(1, order = 1), member(2)).queueStatus())
        assertEquals(QueueStatus.Complete, listOf(member(1, order = 1)).queueStatus())
        assertEquals(QueueStatus.Complete, emptyList<Combatant>().queueStatus())
    }

    @Test
    fun `a death save pip sets the count to itself`() {
        assertEquals(2, toggledSaves(current = 0, index = 1))
    }

    @Test
    fun `pressing the last filled pip empties it`() {
        assertEquals(1, toggledSaves(current = 2, index = 1))
    }

    @Test
    fun `a dragged member gets a number and the pending ones stay behind`() {
        val list = listOf(member(1, order = 1), member(2, order = 2), member(3), member(4))
        val change = checkNotNull(list.movedInQueue(id = 3, toIndex = 0, turn = 0))
        assertEquals(mapOf(3L to 1, 1L to 2, 2L to 3, 4L to null), change.orders)
    }

    @Test
    fun `a member cannot be dropped behind the pending ones`() {
        val list = listOf(member(1, order = 1), member(2), member(3))
        val change = checkNotNull(list.movedInQueue(id = 1, toIndex = 2, turn = 0))
        assertEquals(mapOf(1L to 1, 2L to null, 3L to null), change.orders)
    }

    @Test
    fun `the turn follows the person who moves when the queue is moved`() {
        val list = listOf(member(1, order = 1), member(2, order = 2), member(3, order = 3))
        val change = checkNotNull(list.movedInQueue(id = 3, toIndex = 0, turn = 1))
        assertEquals(2, change.turn)
    }

    @Test
    fun `the turn stays on the mover who was dragged`() {
        val list = listOf(member(1, order = 1), member(2, order = 2), member(3, order = 3))
        val change = checkNotNull(list.movedInQueue(id = 1, toIndex = 2, turn = 0))
        assertEquals(2, change.turn)
    }

    @Test
    fun `an unknown member moves nowhere`() {
        assertNull(listOf(member(1, order = 1)).movedInQueue(id = 9, toIndex = 0, turn = 0))
    }

    @Test
    fun `pinning a pending place names the ones before it as they stand`() {
        val list = listOf(member(1, order = 1), member(2), member(3), member(4))
        assertEquals(mapOf(1L to 1, 2L to 2, 3L to 3, 4L to null), list.pinnedInQueue(3))
    }

    @Test
    fun `the orders are written down for an undo`() {
        assertEquals(mapOf(1L to 2, 2L to null), listOf(member(1, order = 2), member(2)).queueOrders())
    }
}
