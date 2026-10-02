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
import com.slack.circuit.foundation.CircuitCompositionLocals

/** composition root - wires locale, theme and the Circuit instance, then hands off to the navigation shell */
@Composable
fun App(appContainer: AppContainer) {
    val language by appContainer.languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
    LaunchedEffect(language) { customAppLocale = language.tag }

    AppEnvironment {
        AppTheme {
            CircuitCompositionLocals(appContainer.circuit) {
                AppTabHost(appContainer.undoController)
            }
        }
    }
}
