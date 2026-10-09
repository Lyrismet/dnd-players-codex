package com.lyrismet.incadent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.lyrismet.incadent.core.designsystem.AppTheme
import com.lyrismet.incadent.core.designsystem.component.LocalMentionStyle
import com.lyrismet.incadent.core.localization.AppEnvironment
import com.lyrismet.incadent.core.localization.customAppLocale
import com.lyrismet.incadent.di.AppGraph
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.model.MentionStyle
import com.slack.circuit.foundation.CircuitCompositionLocals
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val SPLASH_HOLD_MILLIS = 1500
private const val SPLASH_FADE_MILLIS = 450

private enum class SplashPhase { VISIBLE, FADING, GONE }

/** composition root - wires locale, theme and the Circuit instance, then hands off to the navigation shell */
@Composable
fun App(
    graph: AppGraph,
    onExit: () -> Unit = {},
) {
    val language by graph.languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
    LaunchedEffect(language) { customAppLocale = language.tag }
    val mentionStyle by graph.appPreferencesRepository
        .observeMentionStyle()
        .collectAsState(initial = MentionStyle.FILLED)

    var splashPhase by remember { mutableStateOf(SplashPhase.VISIBLE) }
    LaunchedEffect(Unit) {
        delay(SPLASH_HOLD_MILLIS.toLong().milliseconds)
        splashPhase = SplashPhase.FADING
        delay(SPLASH_FADE_MILLIS.toLong().milliseconds)
        splashPhase = SplashPhase.GONE
    }

    // circuit and tab state sit above AppEnvironment - its key() recomposes everything below on a language change
    CircuitCompositionLocals(graph.circuit) {
        val tabs = rememberAppTabsState()

        AppEnvironment {
            CompositionLocalProvider(LocalMentionStyle provides mentionStyle) {
                AppTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppTabHost(graph.undoController, graph.swipeHintReplayController, tabs, onExit)
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
    }
}
