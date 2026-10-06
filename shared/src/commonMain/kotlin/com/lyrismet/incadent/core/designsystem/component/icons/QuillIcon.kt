package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val QUILL_VIEWBOX = 24f
private const val QUILL_CENTER = 12f
private const val QUILL_ROTATION_DEG = 45f

// the mockup's quill (the ✎ action of session notes and the hold hint) - a 24-unit pen drawn rotated 45 degrees
private val QuillPathData =
    listOf(
        "M11.4,1.8 C9,4.2 8,8 8.4,12 L10.2,11 L8.8,14.2 C9.5,15.4 10.4,16.3 11.4,16.8 Z",
        "M12.6,1.8 C15,4.2 16,7.6 15.6,11 L13.8,10.2 L15.2,13.4 C14.5,15.1 13.6,16.2 12.6,16.8 Z",
        "M11.35,16 H12.65 V21 L12,23 L11.35,21 Z",
    )

/** the quill glyph in a square of [size], filled with [tint] - the default is the gold of the mockup's hint */
@Composable
fun QuillIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = AppPalette.Gold,
) {
    val paths: List<Path> = remember { QuillPathData.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val factor = this.size.width / QUILL_VIEWBOX
        val center = Offset(QUILL_CENTER, QUILL_CENTER)
        withTransform({
            scale(factor, factor, pivot = Offset.Zero)
            rotate(QUILL_ROTATION_DEG, pivot = center)
        }) {
            paths.forEach { drawPath(it, tint) }
        }
    }
}
