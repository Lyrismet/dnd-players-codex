package com.lyrismet.incadent.core.navigation

sealed interface BackAction {
    data object Pop : BackAction

    data object SwitchToSessions : BackAction

    data object ConfirmExit : BackAction
}
