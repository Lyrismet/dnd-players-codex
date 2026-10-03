package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val DOT_COUNT = 3

/** 3 horizontal dots glyph - matches players codex v4.dc.html's settings tab icon */
@Composable
fun SettingsTabIcon(tint: Color) {
    TabIconSlot {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(DOT_COUNT) { Box(Modifier.size(4.dp).background(tint, CircleShape)) }
        }
    }
}
