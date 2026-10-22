package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the standard spinning loader in the accent gold, drawn while something is still being measured or read */
@Composable
fun AppLoader(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 3.dp,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = AppPalette.Gold,
        trackColor = Color.Transparent,
        strokeWidth = strokeWidth,
    )
}
