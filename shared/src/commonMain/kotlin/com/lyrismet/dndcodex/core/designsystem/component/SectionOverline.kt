package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** the tracked all-caps label used for page eyebrows and list section headers (e.g. "АРХИВ", "БАЗА ЗНАНИЙ") */
@Composable
fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.Gold,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier = modifier) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
        trailingContent()
    }
}
