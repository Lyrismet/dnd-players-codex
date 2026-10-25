package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** dashed rounded-rect border, [Modifier.border] has no dash support - dashes are in dp so every density looks alike */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    cornerRadius: Dp = 20.dp,
    dashLength: Dp = 6.dp,
    dashGap: Dp = 4.dp,
): Modifier =
    // the stroke and its dash effect are made once per size and density, not on every redraw
    drawWithCache {
        val radius = CornerRadius(cornerRadius.toPx())
        val stroke =
            Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashGap.toPx())),
            )
        onDrawBehind { drawRoundRect(color = color, cornerRadius = radius, style = stroke) }
    }

/** a dashed rule along the top edge, for a block that continues under a dashed divider */
fun Modifier.dashedTopRule(
    color: Color,
    width: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    dashGap: Dp = 3.dp,
): Modifier =
    drawWithCache {
        val strokePx = width.toPx()
        val dashes = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashGap.toPx()))
        onDrawBehind {
            drawLine(
                color = color,
                start = Offset(0f, strokePx / 2),
                end = Offset(size.width, strokePx / 2),
                strokeWidth = strokePx,
                pathEffect = dashes,
            )
        }
    }
