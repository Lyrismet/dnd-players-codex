package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style =
                Stroke(
                    width = width.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashGap.toPx())),
                ),
        )
    }

/** a dashed rule along the top edge, for a block that continues under a dashed divider */
fun Modifier.dashedTopRule(
    color: Color,
    width: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    dashGap: Dp = 3.dp,
): Modifier =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, width.toPx() / 2),
            end = Offset(size.width, width.toPx() / 2),
            strokeWidth = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashGap.toPx())),
        )
    }
