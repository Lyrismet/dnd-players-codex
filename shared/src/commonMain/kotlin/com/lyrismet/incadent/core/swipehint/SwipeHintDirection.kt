package com.lyrismet.incadent.core.swipehint

import com.lyrismet.incadent.core.preferences.SwipeHintState

/** a list's hint plays once it is pending, has a first row to demo on, and nothing else is covering the screen */
fun shouldStartSwipeHint(
    hintState: SwipeHintState,
    hasFirstItem: Boolean,
    isBlocked: Boolean,
): Boolean = hintState == SwipeHintState.PENDING && hasFirstItem && !isBlocked
