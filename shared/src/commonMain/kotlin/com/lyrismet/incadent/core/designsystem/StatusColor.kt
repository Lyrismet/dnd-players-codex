package com.lyrismet.incadent.core.designsystem

import androidx.compose.ui.graphics.Color

/** foreground/background/border triple for a status chip, e.g. the NPC status badge or a session-note mention */
data class StatusColor(
    val foreground: Color,
    val background: Color,
    val border: Color,
)

fun statusColor(
    foreground: Color,
    background: Color,
    border: Color = foreground.copy(alpha = 0.4f),
): StatusColor = StatusColor(foreground = foreground, background = background, border = border)
