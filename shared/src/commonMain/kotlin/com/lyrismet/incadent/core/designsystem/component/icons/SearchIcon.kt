package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val VIEWPORT = 24f
private const val LENS_CENTER = 11f
private const val LENS_RADIUS = 6.5f
private const val HANDLE_START = 16f
private const val HANDLE_END = 20.5f
private const val STROKE = 2f

/** the loupe of every search field - lens and handle on the design's 24 unit grid, round caps */
@Composable
fun SearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 17.dp,
    tint: Color = AppPalette.TextSecondary,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.width / VIEWPORT
        val stroke = Stroke(width = STROKE * unit, cap = StrokeCap.Round)
        drawCircle(
            color = tint,
            radius = LENS_RADIUS * unit,
            center = Offset(LENS_CENTER * unit, LENS_CENTER * unit),
            style = stroke,
        )
        drawLine(
            color = tint,
            start = Offset(HANDLE_START * unit, HANDLE_START * unit),
            end = Offset(HANDLE_END * unit, HANDLE_END * unit),
            strokeWidth = STROKE * unit,
            cap = StrokeCap.Round,
        )
    }
}
