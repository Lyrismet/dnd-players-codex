package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.DiamondOutline

/** rotated-square outline glyph - matches players codex v4.dc.html's codex tab icon */
@Composable
fun CodexTabIcon(tint: Color) {
    TabIconSlot {
        DiamondOutline(size = 12.dp, color = tint, strokeWidth = TAB_ICON_STROKE_WIDTH)
    }
}
