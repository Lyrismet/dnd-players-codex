package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * the body of a bottom sheet with the design's content inset (20dp sides, 6dp under the handle, 44dp at the bottom,
 * Players Codex v5 `sheetRef`) - put an [AppSheetHeader] first. [scroll] lets a tall body scroll inside the sheet.
 */
@Composable
fun AppSheetColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
    scroll: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}
