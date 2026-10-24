package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import kotlin.test.Test
import kotlin.test.assertEquals

class CalcEditingTest {
    private fun draft() = CalcDraft(mode = CalcMode.DAMAGE, targets = listOf(1), sourceId = 2)

    private fun CalcDraft.press(vararg events: CalcEvent): CalcDraft =
        events.fold(this) { acc, e -> CalcEditing.edit(acc, e) }

    @Test
    fun `digits build a sum with a plus between terms`() {
        val after = draft().press(CalcEvent.Digit(8), CalcEvent.Plus, CalcEvent.Digit(6))
        assertEquals("8+6", after.expr.raw)
    }

    @Test
    fun `a term takes the type chosen before its first digit`() {
        val after = draft().press(CalcEvent.TypeSelected(DamageType.FIRE), CalcEvent.Digit(8))
        assertEquals(listOf(DamageType.FIRE), after.termTypes)
    }

    @Test
    fun `choosing a type after the digits sets it for that term`() {
        val after = draft().press(CalcEvent.Digit(8), CalcEvent.TypeSelected(DamageType.COLD))
        assertEquals(listOf(DamageType.COLD), after.termTypes)
    }

    @Test
    fun `pressing the same type again takes it off`() {
        val after =
            draft().press(
                CalcEvent.Digit(8),
                CalcEvent.TypeSelected(DamageType.COLD),
                CalcEvent.TypeSelected(DamageType.COLD),
            )
        assertEquals(listOf(null), after.termTypes)
        assertEquals(null, after.currentType)
    }

    @Test
    fun `the next term starts with the type of the last choice`() {
        val after =
            draft().press(
                CalcEvent.TypeSelected(DamageType.FIRE),
                CalcEvent.Digit(8),
                CalcEvent.Plus,
                CalcEvent.Digit(4),
            )
        assertEquals(listOf(DamageType.FIRE, DamageType.FIRE), after.termTypes)
    }

    @Test
    fun `a backspace drops the type of the term that is gone`() {
        val after =
            draft().press(
                CalcEvent.Digit(8),
                CalcEvent.TypeSelected(DamageType.FIRE),
                CalcEvent.Plus,
                CalcEvent.Digit(4),
                CalcEvent.Backspace,
                CalcEvent.Backspace,
            )
        assertEquals(listOf(DamageType.FIRE), after.termTypes)
    }

    @Test
    fun `a term never grows past three digits`() {
        val after = draft().press(CalcEvent.Digit(1), CalcEvent.Digit(2), CalcEvent.Digit(3), CalcEvent.Digit(4))
        assertEquals("123", after.expr.raw)
    }

    @Test
    fun `a scale chosen then taken back to full leaves no trace`() {
        val half = draft().press(CalcEvent.ScaleSelected(1, HitScale.HALF))
        assertEquals(mapOf(1L to HitScale.HALF), half.scales)
        assertEquals(emptyMap(), half.press(CalcEvent.ScaleSelected(1, HitScale.ONE)).scales)
    }

    @Test
    fun `removing a target takes its scale with it`() {
        val after = draft().press(CalcEvent.ScaleSelected(1, HitScale.ZERO), CalcEvent.TargetRemoved(1))
        assertEquals(emptyList(), after.targets)
        assertEquals(emptyMap(), after.scales)
    }
}
