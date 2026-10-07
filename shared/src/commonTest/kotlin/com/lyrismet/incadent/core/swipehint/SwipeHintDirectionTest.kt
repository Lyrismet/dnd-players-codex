package com.lyrismet.incadent.core.swipehint

import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.SwipeHintState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeHintDirectionTest {
    @Test
    fun `codex hint is delete-only in quick mode and edit-and-delete in form mode`() {
        assertEquals(SwipeHintDirection.DELETE_ONLY, codexSwipeHintDirection(EntityEditMode.QUICK))
        assertEquals(SwipeHintDirection.EDIT_AND_DELETE, codexSwipeHintDirection(EntityEditMode.FORM))
    }

    @Test
    fun `hint starts when pending with a first item and nothing blocking it`() {
        assertTrue(shouldStartSwipeHint(SwipeHintState.PENDING, hasFirstItem = true, isBlocked = false))
    }

    @Test
    fun `hint does not start once it has already been seen`() {
        assertFalse(shouldStartSwipeHint(SwipeHintState.SEEN, hasFirstItem = true, isBlocked = false))
    }

    @Test
    fun `hint does not start with an empty list`() {
        assertFalse(shouldStartSwipeHint(SwipeHintState.PENDING, hasFirstItem = false, isBlocked = false))
    }

    @Test
    fun `hint does not start while a sheet or the undo toast covers the screen`() {
        assertFalse(shouldStartSwipeHint(SwipeHintState.PENDING, hasFirstItem = true, isBlocked = true))
    }
}
