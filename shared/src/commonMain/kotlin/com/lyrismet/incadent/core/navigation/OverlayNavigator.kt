package com.lyrismet.incadent.core.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.screen.Screen

/** the stack of screens drawn over the whole app, empty while nothing is open, the last one is on top */
class OverlayNavigator : GoToNavigator {
    private val stack = mutableStateListOf<Screen>()

    val top: Screen? get() = stack.lastOrNull()

    override fun goTo(screen: Screen): Boolean = stack.add(screen)

    fun pop() {
        stack.removeLastOrNull()
    }
}

/** where a presenter opens a screen over the app, the app root provides it */
val LocalOverlayNavigator =
    compositionLocalOf<GoToNavigator> { error("no overlay navigator provided") }
