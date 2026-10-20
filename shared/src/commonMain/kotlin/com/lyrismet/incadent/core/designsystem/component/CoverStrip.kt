package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_STRIP_FRACTION = 0.4f

// the fade eases in over the whole strip, so its left part is only a faint tint
private const val FADE_EARLY_STOP = 0.35f
private const val FADE_EARLY_ALPHA = 0.12f
private const val FADE_LATE_STOP = 0.7f
private const val FADE_LATE_ALPHA = 0.5f
private const val FADE_EDGE_PX = 2f

/** a picture cropped into the right [widthFraction] of the box and fading out to its left, behind the content */
fun Modifier.coverStrip(
    cover: ImageBitmap,
    widthFraction: Float = DEFAULT_STRIP_FRACTION,
): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawBehind {
        // whole pixels, so the picture and the fade start on the same column and leave no seam
        val stripWidth = (size.width * widthFraction).roundToInt().toFloat()
        val left = size.width - stripWidth
        // the picture is scaled to fill the strip and cut evenly on the sides that overflow
        val scale = max(stripWidth / cover.width, size.height / cover.height)
        val srcWidth = (stripWidth / scale).roundToInt().coerceIn(1, cover.width)
        val srcHeight = (size.height / scale).roundToInt().coerceIn(1, cover.height)
        drawImage(
            image = cover,
            srcOffset = IntOffset((cover.width - srcWidth) / 2, (cover.height - srcHeight) / 2),
            srcSize = IntSize(srcWidth, srcHeight),
            dstOffset = IntOffset(left.roundToInt(), 0),
            dstSize = IntSize(stripWidth.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
        drawRect(
            brush =
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    FADE_EARLY_STOP to Color.Black.copy(alpha = FADE_EARLY_ALPHA),
                    FADE_LATE_STOP to Color.Black.copy(alpha = FADE_LATE_ALPHA),
                    1f to Color.Black,
                    startX = left,
                    endX = size.width,
                ),
            // starts a little left of the picture, where the gradient is already clear, to cover its edge column
            topLeft = Offset(left - FADE_EDGE_PX, 0f),
            size = Size(stripWidth + FADE_EDGE_PX, size.height),
            blendMode = BlendMode.DstIn,
        )
    }
