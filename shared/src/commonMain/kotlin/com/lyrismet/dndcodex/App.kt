package com.lyrismet.dndcodex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lyrismet.dndcodex.core.designsystem.AppTheme
import com.lyrismet.dndcodex.core.localization.AppEnvironment
import com.lyrismet.dndcodex.core.localization.customAppLocale
import com.lyrismet.dndcodex.data.AppContainer
import com.lyrismet.dndcodex.domain.model.AppLanguage
import com.lyrismet.dndcodex.presentation.sessionlist.SessionListScreen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator

@Composable
fun App(appContainer: AppContainer) {
    val language by appContainer.languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
    LaunchedEffect(language) { customAppLocale = language.tag }

    AppEnvironment {
        AppTheme {
            CircuitCompositionLocals(appContainer.circuit) {
                val backStack = rememberSaveableBackStack(root = SessionListScreen)
                val navigator = rememberCircuitNavigator(backStack, onRootPop = {})
                NavigableCircuitContent(navigator = navigator, backStack = backStack)
            }
        }
    }
}
