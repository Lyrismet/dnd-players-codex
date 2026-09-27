package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** a solid dot with a concentric flat-color glow ring, matching the design's live-indicator box-shadow halo */
@Composable
fun GlowingDot(
    dotSize: Dp,
    modifier: Modifier = Modifier,
    dotColor: Color = AppPalette.EmeraldBright,
    glowColor: Color = AppPalette.Emerald.copy(alpha = 0.25f),
    ringWidth: Dp = 3.dp,
) {
    Box(
        modifier = modifier.size(dotSize + ringWidth * 2).clip(CircleShape).background(glowColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
    }
}
