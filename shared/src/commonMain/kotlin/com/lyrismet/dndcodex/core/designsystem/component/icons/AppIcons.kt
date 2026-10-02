package com.lyrismet.dndcodex.core.designsystem.component.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val ICON_VIEWPORT = 24f
private const val QUILL_ROTATION_DEGREES = 45f
private const val QUILL_PIVOT = 12f

// the quill from the design (v5), two feather halves and a nib, tinted by the Icon's tint
private fun quillIcon(): ImageVector =
    ImageVector
        .Builder(
            name = "Quill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
        ).addGroup(rotate = QUILL_ROTATION_DEGREES, pivotX = QUILL_PIVOT, pivotY = QUILL_PIVOT)
        .addPath(
            pathData = addPathNodes("M11.4,1.8 C9,4.2 8,8 8.4,12 L10.2,11 L8.8,14.2 C9.5,15.4 10.4,16.3 11.4,16.8 Z"),
            fill = SolidColor(Color.Black),
        ).addPath(
            pathData =
                addPathNodes("M12.6,1.8 C15,4.2 16,7.6 15.6,11 L13.8,10.2 L15.2,13.4 C14.5,15.1 13.6,16.2 12.6,16.8 Z"),
            fill = SolidColor(Color.Black),
        ).addPath(
            pathData = addPathNodes("M11.35,16 H12.65 V21 L12,23 L11.35,21 Z"),
            fill = SolidColor(Color.Black),
        ).clearGroup()
        .build()

/** glyphs reused across 2+ features - one place to see, and if needed swap, every reference */
object AppIcons {
    val Edit: ImageVector = quillIcon()
    val Delete: ImageVector = Icons.Outlined.Delete
}
