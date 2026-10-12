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
    fun `an open select sheet or quick-add panel is not a change`() {
        val transient =
            baseline.copy(
                placeSelectField = CodexEntryPlaceField.NPC_PLACES,
                quickAddField = CodexEntryChipField.QUEST_GIVER,
                quickAddDraft = "Таверна",
            )
        assertFalse(transient.hasUnsavedChanges(baseline))
    }
}
