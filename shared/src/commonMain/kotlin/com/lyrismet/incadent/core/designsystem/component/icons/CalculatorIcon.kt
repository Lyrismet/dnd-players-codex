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

/** the number-pad sheet header's glyph - a bordered frame over a 2x2 grid of filled keys */
@Composable
fun CalculatorIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppPalette.GoldBright,
) {
    Canvas(modifier.size(IconWidth, IconHeight)) {
        drawRoundRect(
            color = tint,
            cornerRadius = CornerRadius(OuterRadius.toPx()),
            style = Stroke(width = StrokeWidth.toPx()),
        )
        val padSide = Padding.toPx()
        val gap = Gap.toPx()
        val gridWidth = size.width - padSide * 2
        val gridHeight = size.height - TopPadding.toPx() - padSide
        val cellWidth = (gridWidth - gap) / 2
        val cellHeight = (gridHeight - gap) / 2
        val cellRadius = CornerRadius(KeyRadius.toPx())
        for (row in 0 until 2) {
            for (col in 0 until 2) {
                drawRoundRect(
                    color = tint,
                    topLeft =
                        Offset(
                            padSide + col * (cellWidth + gap),
                            TopPadding.toPx() + row * (cellHeight + gap),
                        ),
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = cellRadius,
                )
            }
        }
    }
}
