package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryChipField
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormFields
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryPlaceField
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodexEntryUnsavedChangesTest {
    private val baseline = CodexEntryFormFields(editingRef = null, type = EntityKind.NPC, name = "Рагнар")

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

    @Test
    fun `confirming the same places in another order is not a change`() {
        val placed = baseline.copy(npcLocationIds = listOf(1, 2, 3))
        assertFalse(placed.copy(npcLocationIds = listOf(3, 1, 2)).hasUnsavedChanges(placed))
        assertTrue(placed.copy(npcLocationIds = listOf(3, 1)).hasUnsavedChanges(placed))
    }
}
