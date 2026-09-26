package com.lyrismet.dndcodex.core.designsystem.component.icons

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val ROTATION_DEGREES = 45f

/** rotated-square outline glyph - matches players codex v4.dc.html's codex tab icon */
@Composable
fun CodexTabIcon(tint: Color) {
    TabIconSlot {
        Box(Modifier.size(12.dp).rotate(ROTATION_DEGREES).border(TAB_ICON_STROKE_WIDTH, tint))
    }
}
