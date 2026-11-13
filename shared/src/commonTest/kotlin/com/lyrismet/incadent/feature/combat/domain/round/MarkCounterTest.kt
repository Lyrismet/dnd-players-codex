package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatMark
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkCounterTest {
    private val mark = CombatMark(1, 1, MarkKind.FACT, "AC 12", roundsLeft = 3, startedRound = 2)

    @Test
    fun `the count goes down with the rounds and stops at zero`() {
        assertEquals(3, mark.remaining(2))
        assertEquals(1, mark.remaining(4))
        assertEquals(0, mark.remaining(5))
        assertEquals(0, mark.remaining(9))
    }

    @Test
    fun `going a round back gives the count back`() {
        assertEquals(1, mark.remaining(4))
        assertEquals(2, mark.remaining(3))
    }

    @Test
    fun `a mark without a count has no remaining rounds`() {
        assertNull(mark.copy(roundsLeft = null).remaining(7))
    }

    @Test
    fun `the last round and the end are told apart`() {
        assertTrue(mark.isLastRound(4))
        assertFalse(mark.isLastRound(3))
        assertTrue(mark.isExpired(5))
    }

    @Test
    fun `a mark is told expired once on the round it runs out`() {
        val member =
            Combatant(
                1,
                "Orc",
                CombatSide.ENEMY,
                null,
                null,
                null,
                true,
                CombatState.WHOLE,
                1,
                0,
                0,
                marks = listOf(mark),
            )
        assertEquals(listOf(mark), listOf(member).expiredBetween(4, 5))
        assertTrue(listOf(member).expiredBetween(5, 6).isEmpty())
        assertTrue(listOf(member).expiredBetween(5, 4).isEmpty())
    }
}
