package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.designsystem.component.icons.QuillIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private val RevealWidth = 96.dp
private val EditIconSize = 20.dp
private val EditLabelGap = 5.dp
private val FullSwipeWidth = 190.dp
private val MaxDragWidth = 300.dp
private val RowCornerRadius = 14.dp
private const val SWIPE_ANIMATION_DURATION_MS = 300

private const val COLLAPSE_ANIMATION_DURATION_MS = 220
private const val REMOVAL_GRACE_MS = 1000L

// top and bottom edges converge on the center as the row collapses, Gmail-delete-style, not a one-sided slide
private fun collapseExit(durationMs: Int = COLLAPSE_ANIMATION_DURATION_MS) =
    shrinkVertically(tween(durationMs), shrinkTowards = Alignment.CenterVertically) + fadeOut(tween(durationMs))

// matches the mockup swipe/snap transition cubic-bezier(.2,.8,.2,1)
private val SwipeEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

// live drag writes a plain float synchronously and the animatable only takes over after release
private class SwipeState(
    val fullWidthPx: Float,
    val maxDragPx: Float,
    // a right swipe only exists when the row has an edit action - otherwise the row can't travel right at all
    val canSwipeRight: Boolean,
) {
    var isDragging by mutableStateOf(false)
    var dragPx by mutableFloatStateOf(0f)
    val settledOffset = Animatable(0f)

    val offsetPx: Float get() = if (isDragging) dragPx else settledOffset.value

    fun onDragStart() {
        isDragging = true
        dragPx = settledOffset.value
    }

    fun onDrag(delta: Float) {
        dragPx = (dragPx + delta).coerceIn(-maxDragPx, if (canSwipeRight) maxDragPx else 0f)
    }

    suspend fun onDragEnd(
        onDeleteRequested: () -> Unit,
        onEditRequested: (() -> Unit)?,
    ) {
        val releasedAt = dragPx
        isDragging = false
        settledOffset.snapTo(releasedAt)
        // a full swipe commits from where the finger left the row - a right swipe opens the editor and settles
        when {
            -releasedAt > fullWidthPx -> onDeleteRequested()
            releasedAt > fullWidthPx && onEditRequested != null -> {
                onEditRequested()
                settle()
            }
            else -> settle()
        }
    }

    // a cancelled gesture is not a release, so it returns to rest and never commits a delete
    suspend fun onDragCancel() {
        val cancelledAt = dragPx
        isDragging = false
        settledOffset.snapTo(cancelledAt)
        settle()
    }

    // rows never rest partially revealed, so every settle goes back to the original position
    suspend fun settle() {
        settledOffset.animateTo(0f, animationSpec = tween(SWIPE_ANIMATION_DURATION_MS, easing = SwipeEasing))
    }

    // brings a row back to rest without animation, for when the delete did not happen after all
    suspend fun reset() {
        settledOffset.snapTo(0f)
    }
}

/** the swipe-right action of a row - its labels are the resting and the fully-pulled captions */
data class SwipeEditAction(
    val label: String,
    val releaseLabel: String,
    val onEdit: () -> Unit,
)

// swipe-left-to-delete row chrome, the caller owns the real delete - a row without editAction stays delete-only
@Composable
fun SwipeToDeleteRow(
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    deleteContentDescription: String? = null,
    editAction: SwipeEditAction? = null,
    content: @Composable () -> Unit,
) {
    val onEditRequested = editAction?.onEdit
    // the gesture outlives recompositions, so it reads the latest edit callback instead of the first one
    val currentOnEdit by rememberUpdatedState(onEditRequested)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state =
        remember(onEditRequested != null) {
            with(density) {
                SwipeState(FullSwipeWidth.toPx(), MaxDragWidth.toPx(), canSwipeRight = onEditRequested != null)
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
            // each side paints only while the row travels towards it - at rest neither background is visible
            if (editAction != null && state.offsetPx > 0f) SwipeEditBackground(state, density, editAction)
            if (state.offsetPx < 0f) SwipeDeleteBackground(state, density, deleteContentDescription)
            Box(
                modifier =
                    Modifier
                        .offset { IntOffset(state.offsetPx.roundToInt(), 0) }
                        .pointerInput(state) {
                            detectHorizontalDragGestures(
                                onDragStart = { state.onDragStart() },
                                onDragEnd = {
                                    scope.launch { state.onDragEnd({ isRemoving = true }, currentOnEdit) }
                                },
                                onDragCancel = { scope.launch { state.onDragCancel() } },
                            ) { change, dragAmount ->
                                change.consume()
                                state.onDrag(dragAmount)
                            }
                        },
            ) {
                content()
            }
        }
    }
}

// the revealed left side of a right swipe - dim gold, brighter past the full-swipe point, quill over the label
@Composable
private fun BoxScope.SwipeEditBackground(
    state: SwipeState,
    density: Density,
    action: SwipeEditAction,
) {
    val isPastFull = state.offsetPx > state.fullWidthPx
    SwipeBackground(
        density = density,
        anchor = Alignment.CenterStart,
        color = if (isPastFull) AppPalette.GoldBright else AppPalette.GoldDim,
        revealPx = state.offsetPx,
    ) {
        QuillIcon(size = EditIconSize, tint = AppPalette.Background)
        Spacer(Modifier.height(EditLabelGap))
        Text(
            if (isPastFull) action.releaseLabel else action.label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = AppPalette.Background,
        )
    }
}

// the revealed right side of a left swipe - maroon, a deeper maroon past the full-swipe point
@Composable
private fun BoxScope.SwipeDeleteBackground(
    state: SwipeState,
    density: Density,
    deleteContentDescription: String?,
) {
    val isPastFull = -state.offsetPx > state.fullWidthPx
    SwipeBackground(
        density = density,
        anchor = Alignment.CenterEnd,
        color = if (isPastFull) AppPalette.MaroonHover else AppPalette.Maroon,
        revealPx = -state.offsetPx,
    ) {
        Icon(AppIcons.Delete, contentDescription = deleteContentDescription, tint = AppPalette.MaroonBright)
    }
}

// the row-sized background of one swipe direction - the row slides over it, so only the side it opens shows.
// the action content keeps its own width (at least RevealWidth), hugs the row edge and is never clipped
@Composable
private fun BoxScope.SwipeBackground(
    density: Density,
    anchor: Alignment,
    color: Color,
    revealPx: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(RowCornerRadius))
                .background(color),
    ) {
        Column(
            modifier =
                Modifier
                    .align(anchor)
                    .width(with(density) { max(RevealWidth.toPx(), revealPx).toDp() })
                    .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
