package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val HOLD_DURATION_MS = 450
private const val RELEASE_DURATION_MS = 150
private val HoldSlop = 8.dp

// the mockup's "transition: width .15s ease" on release
private val ReleaseEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** shown when a press on a [HoldToEditField] ends before the hold completes - the card wires it to a toast */
val LocalHoldTooShort = compositionLocalOf<() -> Unit> { {} }

/**
 * the geometry of a held area - negative margins let the highlight overhang its text, as in the mockup.
 * Each preset is one mockup wrapper: a name, a description or a fact line.
 */
data class HoldFrame(
    val marginHorizontal: Dp,
    val marginVertical: Dp,
    val paddingHorizontal: Dp,
    val paddingVertical: Dp,
    val radius: Dp,
) {
    companion object {
        val Title = HoldFrame(-6.dp, -4.dp, 6.dp, 4.dp, 10.dp)
        val Description = HoldFrame(-6.dp, -6.dp, 6.dp, 6.dp, 10.dp)
        val FactLine = HoldFrame(0.dp, 0.dp, 14.dp, 4.dp, 0.dp)
    }
}

private enum class HoldOutcome { HELD, RELEASED_EARLY, MOVED }

/**
 * a press-and-hold area with a translucent gold fill across its full height that grows over 450 ms.
 * [onHeld] fires on completion - a drag cancels silently, an early release also reports too-short.
 */
@Composable
fun HoldToEditField(
    onHeld: () -> Unit,
    frame: HoldFrame,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnHeld by rememberUpdatedState(onHeld)
    val currentTooShort by rememberUpdatedState(LocalHoldTooShort.current)
    val slopPx = with(LocalDensity.current) { HoldSlop.toPx() }
    Box(
        modifier =
            modifier
                .negativeMargin(frame.marginHorizontal, frame.marginVertical)
                .clip(RoundedCornerShape(frame.radius))
                .drawBehind {
                    if (progress.value > 0f) {
                        drawRect(
                            color = AppPalette.Gold.copy(alpha = 0.16f),
                            size = Size(size.width * progress.value, size.height),
                        )
                    }
                }.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val fill =
                            scope.launch {
                                progress.animateTo(
                                    1f,
                                    tween(HOLD_DURATION_MS, easing = LinearEasing),
                                )
                            }
                        val outcome = awaitHoldOutcome(down.id, down.position, slopPx)
                        fill.cancel()
                        when (outcome) {
                            HoldOutcome.HELD -> {
                                scope.launch { progress.snapTo(0f) }
                                currentOnHeld()
                            }
                            HoldOutcome.RELEASED_EARLY -> {
                                scope.launch {
                                    progress.animateTo(
                                        0f,
                                        tween(RELEASE_DURATION_MS, easing = ReleaseEasing),
                                    )
                                }
                                currentTooShort()
                            }
                            HoldOutcome.MOVED ->
                                scope.launch {
                                    progress.animateTo(
                                        0f,
                                        tween(RELEASE_DURATION_MS, easing = ReleaseEasing),
                                    )
                                }
                        }
                        waitForUpOrCancellation()
                    }
                }.padding(horizontal = frame.paddingHorizontal, vertical = frame.paddingVertical),
    ) {
        content()
    }
}

// lets the child overhang its slot - negative margins shrink the footprint below the child's size
private fun Modifier.negativeMargin(
    horizontal: Dp,
    vertical: Dp,
): Modifier =
    layout { measurable, constraints ->
        val h = horizontal.roundToPx()
        val v = vertical.roundToPx()
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout((placeable.width + 2 * h).coerceAtLeast(0), (placeable.height + 2 * v).coerceAtLeast(0)) {
            placeable.placeRelative(h, v)
        }
    }

private suspend fun AwaitPointerEventScope.awaitHoldOutcome(
    pointerId: PointerId,
    start: Offset,
    slopPx: Float,
): HoldOutcome =
    withTimeoutOrNull(HOLD_DURATION_MS.toLong()) { trackUntilReleased(pointerId, start, slopPx) }
        ?: HoldOutcome.HELD

private suspend fun AwaitPointerEventScope.trackUntilReleased(
    pointerId: PointerId,
    start: Offset,
    slopPx: Float,
): HoldOutcome {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
        val outcome = if (change == null) HoldOutcome.RELEASED_EARLY else holdOutcomeOf(change, start, slopPx)
        if (outcome != null) return outcome
    }
}

// null while the press is still being held in place
private fun holdOutcomeOf(
    change: PointerInputChange,
    start: Offset,
    slopPx: Float,
): HoldOutcome? =
    when {
        !change.pressed -> HoldOutcome.RELEASED_EARLY
        (change.position - start).getDistance() > slopPx -> HoldOutcome.MOVED
        else -> null
    }
