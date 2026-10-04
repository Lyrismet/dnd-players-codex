package com.lyrismet.incadent.core.navigation

/** the active tab unwinds first, then falls back to sessions, and only the sessions root asks before exiting */
fun resolveBackAction(
    isOnSessionsTab: Boolean,
    activeTabDepth: Int,
): BackAction =
    when {
        activeTabDepth > 1 -> BackAction.Pop
        !isOnSessionsTab -> BackAction.SwitchToSessions
        else -> BackAction.ConfirmExit
    }
