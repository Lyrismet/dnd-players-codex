package com.lyrismet.incadent.core.designsystem.component.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val TAB_ICON_SLOT_HEIGHT = 18.dp
internal val TAB_ICON_STROKE_WIDTH = 1.6.dp

// fixed-height slot keeps all 4 differently-sized hand-drawn glyphs aligned to the same label baseline
@Composable
internal fun TabIconSlot(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.height(TAB_ICON_SLOT_HEIGHT), contentAlignment = Alignment.Center, content = content)
}
