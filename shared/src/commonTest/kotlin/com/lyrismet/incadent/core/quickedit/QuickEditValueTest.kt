package com.lyrismet.incadent.core.quickedit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickEditValueTest {
    @Test
    fun `a blank name is not saved`() {
        assertNull(quickEditValueFromDraft(QuickEditField.NAME, "   "))
    }

    @Test
    fun `a name is trimmed before it is saved`() {
        assertEquals(QuickEditValue.Text("Ворон"), quickEditValueFromDraft(QuickEditField.NAME, "  Ворон "))
    }

    @Test
    fun `a blank description is saved as empty text`() {
        assertEquals(QuickEditValue.Text(""), quickEditValueFromDraft(QuickEditField.DESCRIPTION, "  "))
    }

    @Test
    fun `a level above its range is clamped down to the maximum`() {
        assertEquals(QuickEditValue.Number(20), quickEditValueFromDraft(QuickEditField.LEVEL, "25"))
    }

    @Test
    fun `a zero armor class is clamped up to the minimum`() {
        assertEquals(QuickEditValue.Number(1), quickEditValueFromDraft(QuickEditField.ARMOR_CLASS, "0"))
    }

    @Test
    fun `initiative bonus keeps its negative sign and is clamped to its range`() {
        assertEquals(QuickEditValue.Number(-3), quickEditValueFromDraft(QuickEditField.INITIATIVE_BONUS, "-3"))
        assertEquals(QuickEditValue.Number(-5), quickEditValueFromDraft(QuickEditField.INITIATIVE_BONUS, "-9"))
    }

    @Test
    fun `max hp is clamped to 999`() {
        assertEquals(QuickEditValue.Number(999), quickEditValueFromDraft(QuickEditField.HP_MAX, "1500"))
    }

    @Test
    fun `a number that is not a number is not saved`() {
        assertNull(quickEditValueFromDraft(QuickEditField.LEVEL, ""))
        assertNull(quickEditValueFromDraft(QuickEditField.LEVEL, "пять"))
    }

    @Test
    fun `links and choices are never committed from a typed draft`() {
        assertNull(quickEditValueFromDraft(QuickEditField.GIVER, "5"))
        assertNull(quickEditValueFromDraft(QuickEditField.RELATION, "friend"))
    }
}
