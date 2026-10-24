package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private val IconWidth = 16.dp
private val IconHeight = 20.dp
private val StrokeWidth = 1.6.dp
private val OuterRadius = 3.dp
private val Padding = 2.dp
private val TopPadding = 6.dp
private val Gap = 2.dp
private val KeyRadius = 1.dp

/** the number-pad glyph, a frame over a 2x2 grid of keys, the insets scale with [width] and [height] */
@Composable
fun CalculatorIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppPalette.GoldBright,
    width: Dp = IconWidth,
    height: Dp = IconHeight,
) {
    Canvas(modifier.size(width, height)) {
        val scale = height.toPx() / IconHeight.toPx()
        val border = StrokeWidth.toPx()
        // the border sits inside the box and the insets start after it, like the mockup's border-box
        drawRoundRect(
            color = tint,
            topLeft = Offset(border / 2, border / 2),
            size = Size(size.width - border, size.height - border),
            cornerRadius = CornerRadius(OuterRadius.toPx()),
            style = Stroke(width = border),
        )
        val left = border + Padding.toPx() * scale
        val top = border + TopPadding.toPx() * scale
        val gap = Gap.toPx() * scale
        val cellWidth = (size.width - left * 2 - gap) / 2
        val cellHeight = (size.height - top - left - gap) / 2
        val cellRadius = CornerRadius(KeyRadius.toPx())
        for (row in 0 until 2) {
            for (col in 0 until 2) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left + col * (cellWidth + gap), top + row * (cellHeight + gap)),
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = cellRadius,
                )
            }
        }
    }
}
