package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import kotlin.math.hypot

private const val LAP_MILLIS = 4200
private const val RING_ALPHA = 0.35f
private const val FULL_TURN = 360f
private const val SWEEP_START = -90f
private val FrameWidth = 1.5.dp

private val Spark = Color(0xFFFFE9A3)

private const val TAIL_START = 0.8056f
private const val TAIL_MID = 0.9167f
private const val HEAD = 0.9778f
private const val TAIL_ALPHA = 0.5f
private val InnerInset = 4.dp
private val InnerWidth = 1.dp

/** less motion was asked for, animated decorations stand still (no platform hook sets it yet) */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * the one lap of a spark around a frame, 4 seconds long. Every animated frame of a screen reads this single clock, and
 * it is null when motion is reduced so the frame stays solid
 */
@Composable
fun rememberSparkClock(): State<Float>? {
    val transition = rememberInfiniteTransition(label = "spark")
    val lap =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LAP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "lap",
        )
    return if (LocalReduceMotion.current) null else lap
}

/**
 * a gold frame with a comet circling it when there is a [clock], and an optional silver [inner] line inside it. Drawn
 * over the content, so the frame never changes the size of what it surrounds
 */
fun Modifier.sparkFrame(
    cornerRadius: Dp,
    gold: Boolean,
    inner: Color? = null,
    clock: State<Float>? = null,
): Modifier =
    drawWithContent {
        drawContent()
        if (gold) drawFrame(cornerRadius, clock)
        if (inner != null) {
            val inset = InnerInset.toPx()
            drawRoundedOutline(inset, (cornerRadius.toPx() - inset).coerceAtLeast(0f), inner, InnerWidth.toPx())
        }
    }

private fun DrawScope.drawFrame(
    cornerRadius: Dp,
    clock: State<Float>?,
) {
    val width = FrameWidth.toPx()
    val radius = cornerRadius.toPx()
    val ring =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
            val inner =
                RoundRect(
                    width,
                    width,
                    size.width - width,
                    size.height - width,
                    CornerRadius((radius - width).coerceAtLeast(0f)),
                )
            addRoundRect(inner)
        }
    val lap = clock?.value
    drawPath(ring, if (lap == null) AppPalette.Gold else AppPalette.Gold.copy(alpha = RING_ALPHA))
    if (lap == null) return
    val reach = hypot(size.width, size.height)
    clipPath(ring) {
        rotate(degrees = lap * FULL_TURN + SWEEP_START, pivot = center) {
            drawRect(
                brush =
                    Brush.sweepGradient(
                        0f to Color.Transparent,
                        TAIL_START to Color.Transparent,
                        TAIL_MID to AppPalette.GoldBright.copy(alpha = TAIL_ALPHA),
                        HEAD to Spark,
                        1f to Spark.copy(alpha = 0f),
                        center = center,
                    ),
                topLeft = Offset(center.x - reach, center.y - reach),
                size = Size(reach * 2, reach * 2),
            )
        }
    }
}

private fun DrawScope.roundRect(
    inset: Float,
    radius: Float,
) = RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(radius))

private fun DrawScope.drawRoundedOutline(
    inset: Float,
    radius: Float,
    color: Color,
    width: Float,
) {
    val rect = roundRect(inset, radius)
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = rect.topLeftCornerRadius,
        style = Stroke(width),
    )
}
