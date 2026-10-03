package com.lyrismet.incadent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.BrandMark
import com.lyrismet.incadent.core.designsystem.component.FlameSweepEffect
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.splash_app_name
import dndplayerscodex.shared.generated.resources.splash_attribution
import dndplayerscodex.shared.generated.resources.splash_tagline
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// same size as the system splash icon so the mark does not jump when compose takes over
private val SPLASH_MARK_SIZE = 112.dp
private val SPLASH_TEXT_SPACING = 18.dp

// the first text draw pays a one-time layout warm-up, so the text waits before it fades in
private const val TEXT_START_DELAY_MILLIS = 400L
private const val TEXT_FADE_MILLIS = 350

// flip to false to turn off the flame sweep over the logo and text
private const val FLAME_SWEEP_ENABLED = false

/** the sealed-d8 mark, app name and tagline shown on cold start, with only the text fading in */
@Composable
fun Splash(modifier: Modifier = Modifier) {
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(TEXT_START_DELAY_MILLIS)
        textVisible = true
    }
    val textAlpha by animateFloatAsState(if (textVisible) 1f else 0f, tween(TEXT_FADE_MILLIS))

    Box(modifier = modifier.fillMaxSize().background(AppPalette.Background)) {
        // the background must stay outside the flame effect, an opaque fill would tint the whole band solid
        FlameSweepEffect(play = textVisible && FLAME_SWEEP_ENABLED, modifier = Modifier.fillMaxSize()) {
            MarkThenText(
                modifier = Modifier.fillMaxSize(),
                spacing = SPLASH_TEXT_SPACING,
                mark = { BrandMark(size = SPLASH_MARK_SIZE) },
                text = {
                    Column(
                        modifier = Modifier.alpha(textAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SPLASH_TEXT_SPACING),
                    ) {
                        Text(
                            text = stringResource(Res.string.splash_app_name),
                            style = MaterialTheme.typography.displayLarge,
                            color = AppPalette.TextHeading,
                        )
                        Text(
                            text = stringResource(Res.string.splash_tagline).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.24.em),
                            color = AppPalette.TextSecondary,
                        )
                    }
                },
            )
            Text(
                text = stringResource(Res.string.splash_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 46.dp).alpha(textAlpha),
            )
        }
        // starts the text layout warm-up off-screen so the first fade frame is not late
        Text(text = "", modifier = Modifier.size(0.dp))
    }
}

/** centers [mark] independently of [text] so a still-invisible [text] cannot shift it */
@Composable
private fun MarkThenText(
    mark: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
) {
    Layout(modifier = modifier, content = {
        mark()
        text()
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val markPlaceable = measurables[0].measure(loose)
        val textPlaceable = measurables[1].measure(loose)
        val spacingPx = spacing.roundToPx()
        val centerX = constraints.maxWidth / 2
        val centerY = constraints.maxHeight / 2
        layout(constraints.maxWidth, constraints.maxHeight) {
            markPlaceable.place(centerX - markPlaceable.width / 2, centerY - markPlaceable.height / 2)
            textPlaceable.place(
                centerX - textPlaceable.width / 2,
                centerY + markPlaceable.height / 2 + spacingPx,
            )
        }
    }
}
