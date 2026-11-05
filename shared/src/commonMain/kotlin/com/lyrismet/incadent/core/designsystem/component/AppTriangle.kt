package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

private const val ROUNDING = 0.2f

/** a filled triangle with rounded corners, drawn instead of the "▲" glyph that is tiny in some fonts */
@Composable
fun AppTriangle(
    pointsUp: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier.drawWithCache {
            // the path is built once per size, not on every redraw
            val round = minOf(size.width, size.height) * ROUNDING
            val inset = round / 2f
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val path =
                Path().apply {
                    if (pointsUp) {
                        moveTo(size.width / 2f, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                    } else {
                        moveTo(left, top)
                        lineTo(right, top)
                        lineTo(size.width / 2f, bottom)
                    }
                    close()
                }
            // a round-joined stroke over the fill softens the corners, the path is inset to keep the size
            onDrawBehind {
                drawPath(path, color)
                drawPath(path, color, style = Stroke(width = round, join = StrokeJoin.Round))
            }
        },
    )
}
