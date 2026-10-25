package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** a filled triangle drawn instead of the "▲" glyph, which falls back to a tiny one in some fonts */
@Composable
fun AppTriangle(
    pointsUp: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier.drawWithCache {
            // the path is built once per size, not on every redraw
            val path =
                Path().apply {
                    if (pointsUp) {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                    }
                    close()
                }
            onDrawBehind { drawPath(path, color) }
        },
    )
}
