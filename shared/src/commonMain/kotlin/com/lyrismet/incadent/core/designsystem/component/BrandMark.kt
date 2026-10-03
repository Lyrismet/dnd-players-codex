package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

// source coordinates sit in a 100x100 box - scale factor maps them onto the canvas size given by modifier
private const val SOURCE_BOX_SIZE = 100f

// exact design coordinates lifted from the brandbook svg, not arbitrary constants
@Suppress("MagicNumber")
private fun sealedD8Path(scale: Float): Path =
    Path().apply {
        moveTo(50f * scale, 4f * scale)
        lineTo(90f * scale, 44f * scale)
        lineTo(62f * scale, 44f * scale)
        lineTo(50f * scale, 56f * scale)
        lineTo(38f * scale, 44f * scale)
        lineTo(10f * scale, 44f * scale)
        close()
        moveTo(6f * scale, 52f * scale)
        lineTo(34.7f * scale, 52f * scale)
        lineTo(50f * scale, 67.3f * scale)
        lineTo(65.3f * scale, 52f * scale)
        lineTo(94f * scale, 52f * scale)
        lineTo(50f * scale, 96f * scale)
        close()
    }

/** the "sealed d8" brand mark - two locking halves of a d8, used on the splash and the app icon */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    tint: Color = AppPalette.Gold,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / SOURCE_BOX_SIZE
        drawPath(path = sealedD8Path(scale), color = tint)
    }
}
