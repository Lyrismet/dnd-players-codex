package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** overline + serif title + optional ornamental divider - the top of every top-level screen (Сессии, Кодекс, Настройки...) */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    overlineTrailingContent: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 14.dp)) {
        SectionOverline(
            text = overline,
            modifier = Modifier.height(32.dp),
            trailingContent = overlineTrailingContent,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = AppPalette.TextHeading,
        )
        if (showDivider) {
            HeaderDivider(modifier = Modifier.padding(top = 12.dp))
        }
    }
}
