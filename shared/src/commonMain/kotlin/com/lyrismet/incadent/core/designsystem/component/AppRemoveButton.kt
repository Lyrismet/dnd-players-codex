package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** the quiet "✕" at the end of a row that takes the row out of a list, a 40dp target with no fill of its own */
@Composable
fun AppRemoveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    IconBadge(
        modifier = modifier,
        size = size,
        shape = AppShapes.Control,
        background = Color.Transparent,
        onClick = onClick,
    ) {
        Text("✕", fontSize = AppFontSize.Body, color = AppPalette.TextSecondary)
    }
}
