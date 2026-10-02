package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** a plain solid circle - the filter-chip status dot and [GlowingDot]'s own core are both just this at a size */
@Composable
fun Dot(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier.size(size).clip(CircleShape).background(color).let {
                if (borderColor != null) it.border(borderWidth, borderColor, CircleShape) else it
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
