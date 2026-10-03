package com.lyrismet.incadent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.lyrismet.incadent.core.designsystem.AppTheme
import com.lyrismet.incadent.core.localization.AppEnvironment
import com.lyrismet.incadent.core.localization.customAppLocale
import com.lyrismet.incadent.data.AppContainer
import com.lyrismet.incadent.domain.model.AppLanguage
import com.slack.circuit.foundation.CircuitCompositionLocals
import kotlinx.coroutines.delay

private const val SPLASH_HOLD_MILLIS = 1500
private const val SPLASH_FADE_MILLIS = 450

private enum class SplashPhase { VISIBLE, FADING, GONE }

/** composition root - wires locale, theme and the Circuit instance, then hands off to the navigation shell */
@Composable
fun App(appContainer: AppContainer) {
    val language by appContainer.languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
    LaunchedEffect(language) { customAppLocale = language.tag }

    var splashPhase by remember { mutableStateOf(SplashPhase.VISIBLE) }
    LaunchedEffect(Unit) {
        delay(SPLASH_HOLD_MILLIS.toLong())
        splashPhase = SplashPhase.FADING
        delay(SPLASH_FADE_MILLIS.toLong())
        splashPhase = SplashPhase.GONE
    }

    AppEnvironment {
        AppTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CircuitCompositionLocals(appContainer.circuit) {
                    AppTabHost(appContainer.undoController)
                }
                if (splashPhase != SplashPhase.GONE) {
                    val splashAlpha by animateFloatAsState(
                        targetValue = if (splashPhase == SplashPhase.VISIBLE) 1f else 0f,
                        animationSpec = tween(SPLASH_FADE_MILLIS),
                    )
                    Splash(modifier = Modifier.alpha(splashAlpha))
                }
            }
        }
    }
}
