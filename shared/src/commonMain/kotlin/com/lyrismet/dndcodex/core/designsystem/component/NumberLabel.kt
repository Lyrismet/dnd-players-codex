package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** centered fixed-width index (session number, note number) in front of a list row */
@Composable
fun NumberLabel(
    text: String,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    color: Color = AppPalette.Gold,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.width(width),
    )
}
