package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultFadeEdge = 24.dp

/** fades the top and bottom edge of a vertical scroller, each only while there is more to scroll that way */
fun Modifier.scrollEdgeFade(
    state: ScrollableState,
    edge: Dp = DefaultFadeEdge,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val fade = minOf(edge.toPx(), size.height / 2)
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush =
                    Brush.verticalGradient(
                        listOf(Color.Black, Color.Transparent),
                        startY = size.height - fade,
                        endY = size.height,
                    ),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn,
            )
        }
    }
