package com.lyrismet.dndcodex

import androidx.compose.runtime.Composable
import com.lyrismet.dndcodex.core.designsystem.AppTheme
import com.lyrismet.dndcodex.presentation.npclist.NpcListScreen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator

@Composable
fun App(circuit: Circuit) {
    AppTheme {
        CircuitCompositionLocals(circuit) {
            val backStack = rememberSaveableBackStack(root = NpcListScreen)
            val navigator = rememberCircuitNavigator(backStack, onRootPop = {})
            NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
    }
}
