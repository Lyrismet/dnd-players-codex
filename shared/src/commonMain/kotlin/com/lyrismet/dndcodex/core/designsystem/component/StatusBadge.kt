package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.dndcodex.core.designsystem.StatusColor

/** status pill built on TagChip with its border filled in - NPC/quest status badges in the codex */
@Composable
fun StatusBadge(
    text: String,
    color: StatusColor,
    modifier: Modifier = Modifier,
) {
    TagChip(
        text = text,
        modifier = modifier,
        foreground = color.foreground,
        background = color.background,
        border = color.border,
    )
}
