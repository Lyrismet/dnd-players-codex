package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongRestTest {
    private val hurt =
        CharacterVitals(hpMax = 28, hpCurrent = 22, tempHp = 3, deathSavesSuccess = 1, deathSavesFailure = 2)
    private val slot = CharacterResource(1, "Slots", max = 3, current = 1, slotLevel = 1)
    private val rage = CharacterResource(2, "Rage", max = 2, current = 0)
    private val full = CharacterResource(3, "Wild Shape", max = 2, current = 2)

    @Test
    fun `a rest fills the hit points and clears the temporary ones and the death saves`() {
        val plan = LongRest.plan(hurt, emptyList())
        assertEquals(28, plan.vitals.hpCurrent)
        assertEquals(0, plan.vitals.tempHp)
        assertEquals(0, plan.vitals.deathSavesSuccess)
        assertEquals(0, plan.vitals.deathSavesFailure)
        assertEquals(22, plan.summary.hpBefore)
        assertEquals(28, plan.summary.hpAfter)
        assertFalse(plan.summary.hpFull)
    }

    @Test
    fun `a rest refills every slot and resource`() {
        val plan = LongRest.plan(hurt, listOf(slot, rage, full))
        assertEquals(listOf(3, 2, 2), plan.resources.map { it.current })
        assertTrue(plan.summary.hasSpellSlots)
        assertEquals(2, plan.summary.resourceCount)
    }

    @Test
    fun `full hit points are reported as full`() {
        val plan = LongRest.plan(hurt.copy(hpCurrent = 28), emptyList())
        assertTrue(plan.summary.hpFull)
    }

    @Test
    fun `unset hit points stay unset`() {
        val plan = LongRest.plan(CharacterVitals(hpMax = null, hpCurrent = null), emptyList())
        assertEquals(null, plan.vitals.hpCurrent)
        assertFalse(plan.summary.hpFull)
    }

    @Test
    fun `the undo keeps only what the rest changed`() {
        val plan = LongRest.plan(hurt, listOf(slot, rage, full))
        assertEquals(hurt, plan.undo.vitals)
        assertEquals(mapOf(1L to 1, 2L to 0), plan.undo.resourceCounts)
    }
}
