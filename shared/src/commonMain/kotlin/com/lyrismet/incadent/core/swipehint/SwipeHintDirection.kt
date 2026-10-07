package com.lyrismet.incadent.core.swipehint

import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.SwipeHintState

/** whether the one-time teaching peek also plays a right "edit" leg before its left "delete" one */
enum class SwipeHintDirection {
    DELETE_ONLY,
    EDIT_AND_DELETE,
}

/** codex only grows the edit leg in form edit mode - quick mode edits by holding a field, not swiping right */
fun codexSwipeHintDirection(editMode: EntityEditMode): SwipeHintDirection =
    if (editMode == EntityEditMode.FORM) SwipeHintDirection.EDIT_AND_DELETE else SwipeHintDirection.DELETE_ONLY

/** a list's hint plays once it is pending, has a first row to demo on, and nothing else is covering the screen */
fun shouldStartSwipeHint(
    hintState: SwipeHintState,
    hasFirstItem: Boolean,
    isBlocked: Boolean,
): Boolean = hintState == SwipeHintState.PENDING && hasFirstItem && !isBlocked
