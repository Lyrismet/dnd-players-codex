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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.lyrismet.incadent.core.designsystem.AppTheme
import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.localization.AppEnvironment
import com.lyrismet.incadent.core.localization.customAppLocale
import com.lyrismet.incadent.core.navigation.LocalOverlayNavigator
import com.lyrismet.incadent.core.navigation.OverlayNavigator
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.preferences.MentionStyle
import com.lyrismet.incadent.di.AppGraph
import com.lyrismet.incadent.feature.campaign.presentation.CampaignLibraryScreen
import com.lyrismet.incadent.feature.campaign.presentation.exchange.ImportScreen
import com.lyrismet.incadent.feature.mention.presentation.LocalMentionStyle
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
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

    // null until the first emission - the splash covers it, so the library never flashes before the tabs
    val campaigns by graph.campaignRepository.observeAll().collectAsState(initial = null)
    val currentCampaignId by graph.campaignRepository
        .observeCurrent()
        .map { it?.id }
        .collectAsState(initial = null)

    // circuit and tab state sit above AppEnvironment - its key() recomposes everything below on a language change.
    // A campaign switch rebuilds the tabs, so no screen of the old campaign stays on a back stack.
    CircuitCompositionLocals(graph.circuit) {
        key(currentCampaignId) {
            val tabs = rememberAppTabsState()
            // the export and import sheets, a new campaign rebuilds this block and closes them with it
            val overlay = remember { OverlayNavigator() }

            AppEnvironment {
                CompositionLocalProvider(
                    LocalMentionStyle provides mentionStyle,
                    LocalOverlayNavigator provides overlay,
                ) {
                    AppTheme {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when {
                                currentCampaignId != null ->
                                    AppTabHost(graph.undoController, graph.swipeHintReplayController, tabs, onExit)
                                campaigns != null -> CircuitContent(CampaignLibraryScreen)
                            }
                            ExchangeOverlay(overlay, graph.importSource)
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
}

/** the screens opened over the app, and the import sheet for a file that arrives from outside or from a picker */
@Composable
private fun ExchangeOverlay(
    overlay: OverlayNavigator,
    importSource: ImportSource,
) {
    val file by importSource.file.collectAsState()
    LaunchedEffect(file) {
        if (file != null && overlay.top != ImportScreen) overlay.goTo(ImportScreen)
    }
    val open = overlay.top ?: return
    key(open) { CircuitContent(open, onNavEvent = { if (it is NavEvent.Pop) overlay.pop() }) }
}
