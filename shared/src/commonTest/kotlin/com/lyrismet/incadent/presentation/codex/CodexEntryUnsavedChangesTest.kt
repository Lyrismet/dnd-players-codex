package com.lyrismet.incadent.presentation.codex

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodexEntryUnsavedChangesTest {
    private val baseline = CodexEntryFormFields(editingRef = null, type = CodexEntryType.NPC, name = "Рагнар")

    @Test
    fun `no baseline means nothing to lose`() {
        assertFalse(baseline.hasUnsavedChanges(null))
    }

    @Test
    fun `an untouched form has no unsaved changes`() {
        assertFalse(baseline.hasUnsavedChanges(baseline))
    }

    @Test
    fun `a typed value counts as a change`() {
        assertTrue(baseline.copy(name = "Рагнар Каменный").hasUnsavedChanges(baseline))
    }

    @Test
    fun `an open calculator or quick-add panel is not a change`() {
        val transient =
            baseline.copy(
                calculatorField = CodexEntryNumberField.PARTY_LEVEL,
                quickAddField = CodexEntryChipField.NPC_LOCATION,
                quickAddDraft = "Таверна",
            )
        assertFalse(transient.hasUnsavedChanges(baseline))
    }
}
