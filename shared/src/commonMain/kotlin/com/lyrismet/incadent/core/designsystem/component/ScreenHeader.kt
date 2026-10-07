package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.QuillIcon

/** overline + serif title + optional divider - top of every top-level screen (Сессии, Кодекс...) */
@Composable
fun ScreenHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    // makes the overline editable - it gets a quill after the text and tapping either opens the edit
    onOverlineClick: (() -> Unit)? = null,
    overlineTrailingContent: @Composable RowScope.() -> Unit = {},
) {
    val overlineQuill: (@Composable () -> Unit)? =
        if (onOverlineClick != null) {
            { QuillIcon(size = 16.dp, modifier = Modifier.padding(start = 7.dp)) }
        } else {
            null
        }
    Column(modifier = modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 14.dp)) {
        SectionOverline(
            text = overline,
            modifier = Modifier.height(32.dp),
            onClick = onOverlineClick,
            inlineContent = overlineQuill,
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
