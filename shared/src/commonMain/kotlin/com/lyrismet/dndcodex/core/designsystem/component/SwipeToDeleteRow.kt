package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.icons.AppIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.swipe_delete_label
import dndplayerscodex.shared.generated.resources.swipe_delete_release_label
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

private val OpenWidth = 96.dp
private val FullSwipeWidth = 190.dp
private val MaxDragWidth = 300.dp
private val RowCornerRadius = 14.dp
private const val SWIPE_ANIMATION_DURATION_MS = 300

private const val COLLAPSE_ANIMATION_DURATION_MS = 220
private const val REMOVAL_GRACE_MS = 1000L

// top and bottom edges converge on the center as the row collapses, Gmail-delete-style, not a one-sided slide
private fun collapseExit(durationMs: Int = COLLAPSE_ANIMATION_DURATION_MS) =
    shrinkVertically(tween(durationMs), shrinkTowards = Alignment.CenterVertically) + fadeOut(tween(durationMs))

// matches the mockup's swipe/snap transition: cubic-bezier(.2,.8,.2,1)
private val SwipeEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

// live drag writes a plain float synchronously and the animatable only takes over after release
private class SwipeState(
    val openWidthPx: Float,
    val fullWidthPx: Float,
    val maxDragPx: Float,
) {
    var isOpen by mutableStateOf(false)
    var isDragging by mutableStateOf(false)
    var dragPx by mutableFloatStateOf(0f)
    val settledOffset = Animatable(0f)

    val offsetPx: Float get() = if (isDragging) dragPx else settledOffset.value

    fun onDragStart() {
        isDragging = true
        dragPx = settledOffset.value
    }

    fun onDrag(delta: Float) {
        dragPx = (dragPx + delta).coerceIn(-maxDragPx, 0f)
    }

    suspend fun onDragEnd(onDeleteRequested: () -> Unit) {
        val releasedAt = dragPx
        isDragging = false
        settledOffset.snapTo(releasedAt)
        when {
            // a full swipe commits the delete from where the finger left the row, no snap back
            -releasedAt > fullWidthPx -> onDeleteRequested()

            -releasedAt > openWidthPx / 2 -> settle(-openWidthPx, true)

            else -> settle(0f, false)
        }
    }

    // a cancelled gesture is not a release, so it returns to where the row was and never commits a delete
    suspend fun onDragCancel() {
        val cancelledAt = dragPx
        isDragging = false
        settledOffset.snapTo(cancelledAt)
        if (isOpen) settle(-openWidthPx, true) else settle(0f, false)
    }

    suspend fun settle(
        target: Float,
        open: Boolean,
    ) {
        isOpen = open
        settledOffset.animateTo(target, animationSpec = tween(SWIPE_ANIMATION_DURATION_MS, easing = SwipeEasing))
    }

    // brings a row back to rest without animation, for when the delete did not happen after all
    suspend fun reset() {
        isOpen = false
        settledOffset.snapTo(0f)
    }
}

// swipe-left-to-reveal-delete row chrome - caller owns the real delete, usually immediate with an undo toast
@Composable
fun SwipeToDeleteRow(
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    deleteContentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state =
        remember {
            with(density) {
                SwipeState(OpenWidth.toPx(), FullSwipeWidth.toPx(), MaxDragWidth.toPx())
            }
        }
    // collapses in place first, Gmail-style, instead of just vanishing once the row is actually deleted
    var isRemoving by remember { mutableStateOf(false) }
    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(COLLAPSE_ANIMATION_DURATION_MS.toLong())
            onDeleteRequested()
            // still composed after the grace period means nothing was deleted, so bring the row back
            delay(REMOVAL_GRACE_MS)
            state.reset()
            isRemoving = false
        }
    }

    AnimatedVisibility(
        visible = !isRemoving,
        enter = EnterTransition.None,
        exit = collapseExit(),
        modifier = modifier,
    ) {
        Box {
            SwipeDeleteBackground(state, density, deleteContentDescription) { isRemoving = true }
            Box(
                modifier =
                    Modifier
                        .offset { IntOffset(state.offsetPx.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { state.onDragStart() },
                                onDragEnd = { scope.launch { state.onDragEnd { isRemoving = true } } },
                                onDragCancel = { scope.launch { state.onDragCancel() } },
                            ) { change, dragAmount ->
                                change.consume()
                                state.onDrag(dragAmount)
                            }
                        },
            ) {
                content()
            }
            // tap-to-close catcher - only intercepts while revealed, so a closed row's own click still reaches content
            if (state.isOpen) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .offset { IntOffset(state.offsetPx.roundToInt(), 0) }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { scope.launch { state.settle(0f, false) } },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeDeleteBackground(
    state: SwipeState,
    density: Density,
    deleteContentDescription: String?,
    onClick: () -> Unit,
) {
    val isPastFull = -state.offsetPx > state.fullWidthPx
    Row(
        modifier =
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(RowCornerRadius))
                .background(if (isPastFull) AppPalette.MaroonHover else AppPalette.Maroon)
                .clickable(
                    enabled = state.isOpen,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier =
                Modifier
                    .width(with(density) { max(state.openWidthPx, -state.offsetPx).toDp() })
                    .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(AppIcons.Delete, contentDescription = deleteContentDescription, tint = Color.White)
                Text(
                    text =
                        stringResource(
                            if (isPastFull) Res.string.swipe_delete_release_label else Res.string.swipe_delete_label,
                        ),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
