package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** a solid dot with a concentric flat-color glow ring, matching the design's live-indicator box-shadow halo */
@Composable
fun GlowingDot(
    dotSize: Dp,
    modifier: Modifier = Modifier,
    dotColor: Color = AppPalette.EmeraldBright,
    glowColor: Color = AppPalette.Emerald.copy(alpha = 0.25f),
    ringWidth: Dp = 3.dp,
) {
    Dot(size = dotSize + ringWidth * 2, color = glowColor, modifier = modifier) {
        Dot(size = dotSize, color = dotColor)
    }
}
