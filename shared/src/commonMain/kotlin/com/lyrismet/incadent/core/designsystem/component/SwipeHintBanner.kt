package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// the mockup's showHint/maybeHint choreography (lines ~1339-1357) - all times are absolute from the trigger
private const val PEEK_SHOW_DELAY_MS = 700L
private const val BOTH_FIRST_LEG_MS = 700L // 700 -> 1400
private const val BOTH_GAP_MS = 600L // 1400 -> 2000
private const val BOTH_SECOND_LEG_MS = 700L // 2000 -> 2700
private const val ONE_LEG_MS = 800L // 700 -> 1500
private const val BANNER_TOTAL_MS = 9000L

/** the live peek offset and banner visibility of one in-flight swipe hint - see [rememberSwipeHintPlayback] */
@Stable
class SwipeHintPlayback internal constructor(
    private val scope: CoroutineScope,
) {
    // an Animatable, not a raw float - the peek rolls with the same easing/duration as a real swipe settle
    internal val offsetAnimatable = Animatable(0f)
    val peekOffsetPx: Float get() = offsetAnimatable.value
    var bannerVisible by mutableStateOf(false)
        internal set
    internal var sequenceJob: Job? = null

    /** hides the banner and stops the peek immediately on "Понятно" - the one-time flag was already persisted */
    fun dismiss() {
        bannerVisible = false
        sequenceJob?.cancel()
        sequenceJob = null
        scope.launch { offsetAnimatable.snapTo(0f) }
    }
}

/**
 * drives one list's teaching peek and its bottom banner for [targetKey] (null plays nothing) - a one-shot
 * sequence keyed on the target, so it never replays until the caller hands it a fresh target. Every leg of
 * the choreography below fires at the same absolute delay as before - only how the peek rolls between two
 * points changed, from an instant jump to the row's own [SWIPE_ANIMATION_DURATION_MS]/[SwipeEasing] settle,
 * launched alongside (not awaited by) the next delay so the schedule itself is untouched
 */
@Composable
fun rememberSwipeHintPlayback(
    targetKey: Any?,
    direction: SwipeHintDirection,
): SwipeHintPlayback {
    val scope = rememberCoroutineScope()
    val playback = remember { SwipeHintPlayback(scope) }
    val density = LocalDensity.current
    LaunchedEffect(targetKey) {
        if (targetKey == null) return@LaunchedEffect
        // the whole sequence runs as one child job so playback.dismiss() can cancel every leg at once
        playback.sequenceJob =
            launch {
                val peekDistancePx = with(density) { RevealWidth.toPx() }
                val peekSpec = tween<Float>(SWIPE_ANIMATION_DURATION_MS, easing = SwipeEasing)

                fun animateTo(target: Float) = launch { playback.offsetAnimatable.animateTo(target, peekSpec) }

                delay(PEEK_SHOW_DELAY_MS)
                playback.bannerVisible = true
                val firstTarget =
                    if (direction == SwipeHintDirection.EDIT_AND_DELETE) peekDistancePx else -peekDistancePx
                animateTo(firstTarget)
                if (direction == SwipeHintDirection.EDIT_AND_DELETE) {
                    delay(BOTH_FIRST_LEG_MS)
                    animateTo(0f)
                    delay(BOTH_GAP_MS)
                    animateTo(-peekDistancePx)
                    delay(BOTH_SECOND_LEG_MS)
                    animateTo(0f)
                    delay(BANNER_TOTAL_MS - PEEK_SHOW_DELAY_MS - BOTH_FIRST_LEG_MS - BOTH_GAP_MS - BOTH_SECOND_LEG_MS)
                } else {
                    delay(ONE_LEG_MS)
                    animateTo(0f)
                    delay(BANNER_TOTAL_MS - PEEK_SHOW_DELAY_MS - ONE_LEG_MS)
                }
                playback.bannerVisible = false
            }
    }
    return playback
}

/** the floating "swipe to..." teaching banner above the bottom nav - gold/maroon badges match [direction] */
@Composable
fun SwipeHintBanner(
    visible: Boolean,
    direction: SwipeHintDirection,
    title: String,
    subtitle: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppPalette.SurfaceElevated)
                    .border(1.dp, AppPalette.Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SwipeHintBadges(direction)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = AppPalette.TextHeading,
                )
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 1.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = AppPalette.TextSecondary,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                Text(
                    dismissLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppPalette.GoldBright,
                )
            }
        }
    }
}

// the mockup draws plain arrow glyphs on a colored plate here, not the quill/bin icons the real swipe uses
@Composable
private fun SwipeHintBadges(direction: SwipeHintDirection) {
    when (direction) {
        SwipeHintDirection.EDIT_AND_DELETE ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                SwipeHintArrowBadge(
                    symbol = "→",
                    width = 30.dp,
                    background = AppPalette.GoldDim,
                    textColor = AppPalette.Background,
                )
                SwipeHintArrowBadge(
                    symbol = "←",
                    width = 30.dp,
                    background = AppPalette.Maroon,
                    textColor = Color.White,
                )
            }

        SwipeHintDirection.DELETE_ONLY ->
            SwipeHintArrowBadge(
                symbol = "‹‹",
                width = 40.dp,
                background = AppPalette.Maroon,
                textColor = Color.White,
                letterSpacing = (-2).sp,
            )
    }
}

@Composable
private fun SwipeHintArrowBadge(
    symbol: String,
    width: Dp,
    background: Color,
    textColor: Color,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
        )
    }
}
