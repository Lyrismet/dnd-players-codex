package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** circle-with-center-dot glyph - matches players codex v4.dc.html's combat tab icon (no round-count badge yet) */
@Composable
fun CombatTabIcon(tint: Color) {
    TabIconSlot {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(18.dp).border(TAB_ICON_STROKE_WIDTH, tint, CircleShape))
            Box(Modifier.size(6.dp).background(tint, CircleShape))
        }
    }
}
