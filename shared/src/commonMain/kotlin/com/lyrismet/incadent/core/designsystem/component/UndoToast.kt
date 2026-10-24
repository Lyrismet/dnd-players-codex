package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.undo.UNDO_WINDOW_MILLIS
import com.lyrismet.incadent.core.undo.UndoAction
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_undo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

// caller keeps this composed always and just toggles action to null/non-null
@Composable
fun UndoToast(
    action: UndoAction?,
    onUndo: () -> Unit,
    onFollowUp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipe = rememberSwipeAway(action, onDismiss)
    FloatingBar(
        value = action,
        modifier = modifier.then(swipe),
        // no padding on the card itself, the countdown line runs along its whole bottom edge
        contentPadding = PaddingValues(),
    ) { current ->
        UndoToastContent(current, onUndo, onFollowUp)
    }
}

private const val DISMISS_FRACTION = 0.35f

/** drags the bar sideways, a pull of about a third of its width sends it away, a shorter one lets it spring back */
@Composable
private fun rememberSwipeAway(
    action: UndoAction?,
    onDismiss: () -> Unit,
): Modifier {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableIntStateOf(0) }
    val currentDismiss by rememberUpdatedState(onDismiss)
    // a new bar starts in its place, whatever the last one was swiped to
    LaunchedEffect(action?.onUndo) { if (action != null) offset.snapTo(0f) }
    return Modifier
        .onSizeChanged { widthPx = it.width }
        .offset { IntOffset(offset.value.roundToInt(), 0) }
        .graphicsLayer { alpha = 1f - (abs(offset.value) / maxOf(widthPx, 1)).coerceIn(0f, 1f) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        if (abs(offset.value) > widthPx * DISMISS_FRACTION) {
                            offset.animateTo(widthPx * sign(offset.value), tween(DISMISS_SLIDE_MS))
                            currentDismiss()
                        } else {
                            offset.animateTo(0f, tween(DISMISS_SLIDE_MS))
                        }
                    }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f, tween(DISMISS_SLIDE_MS)) } },
            ) { change, dragAmount ->
                change.consume()
                scope.launch { offset.snapTo(offset.value + dragAmount) }
            }
        }
}

private const val DISMISS_SLIDE_MS = 160

private val CountdownHeight = 3.dp
private val SparkRadius = 3.dp
private val SparkGlowRadius = 9.dp

/** the gold line that runs out over the undo window, a small spark at its end shows where the time is */
@Composable
private fun CountdownLine(
    action: UndoAction,
    modifier: Modifier = Modifier,
) {
    val progress = remember(action.onUndo) { Animatable(1f) }
    LaunchedEffect(progress) {
        progress.animateTo(0f, tween(UNDO_WINDOW_MILLIS.toInt(), easing = LinearEasing))
    }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SparkGlowRadius * 2)
                .drawBehind {
                    // read here, while drawing, so the countdown never recomposes the bar
                    val left = progress.value
                    val barHeight = CountdownHeight.toPx()
                    val center = size.height / 2
                    val end = size.width * left
                    drawRect(
                        color = AppPalette.Gold,
                        topLeft = Offset(0f, center - barHeight / 2),
                        size = Size(end, barHeight),
                    )
                    if (left > 0f) {
                        val glow = SparkGlowRadius.toPx()
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(AppPalette.GoldBright.copy(alpha = 0.7f), Color.Transparent),
                                    center = Offset(end, center),
                                    radius = glow,
                                ),
                            radius = glow,
                            center = Offset(end, center),
                        )
                        drawCircle(AppPalette.GoldBright, SparkRadius.toPx(), Offset(end, center))
                    }
                },
    )
}

@Composable
private fun UndoToastContent(
    action: UndoAction,
    onUndo: () -> Unit,
    onFollowUp: () -> Unit,
) {
    Box {
        UndoRow(action, onUndo, onFollowUp)
        CountdownLine(action, Modifier.align(Alignment.BottomStart).offset(y = SparkGlowRadius - CountdownHeight / 2))
    }
}

@Composable
private fun UndoRow(
    action: UndoAction,
    onUndo: () -> Unit,
    onFollowUp: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp + CountdownHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            size = 32.dp,
            shape = AppShapes.Tile,
            background = AppPalette.Maroon.copy(alpha = 0.25f),
        ) {
            Icon(
                if (action.isMade) AppIcons.Edit else AppIcons.Delete,
                contentDescription = null,
                tint = if (action.isMade) AppPalette.GoldBright else AppPalette.MaroonBright,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(action.title, style = MaterialTheme.typography.titleSmall, color = AppPalette.TextHeading)
            Text(
                action.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action.followUp?.let { followUp ->
            TextButton(onClick = onFollowUp, modifier = Modifier.height(44.dp)) {
                Text(followUp.label, color = AppPalette.GoldBright, style = MaterialTheme.typography.titleSmall)
            }
        }
        TextButton(onClick = onUndo, modifier = Modifier.height(44.dp)) {
            Text(
                stringResource(Res.string.action_undo),
                color = AppPalette.GoldBright,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
