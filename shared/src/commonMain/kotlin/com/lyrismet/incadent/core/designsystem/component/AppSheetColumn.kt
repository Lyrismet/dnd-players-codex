package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/**
 * the body of a bottom sheet with the design's content inset (20dp sides, 6dp under the handle, 16dp at the bottom,
 * Players Codex v5 `sheetRef`) - put an [AppSheetHeader] first. [scroll] lets a tall body scroll inside the sheet.
 */
@Composable
fun AppSheetColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
    scroll: Boolean = false,
    bottomPadding: Dp = 16.dp,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Box(Modifier.padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 8.dp)) { header() }
            AppDivider(color = if (scroll && scrollState.value > 0) AppPalette.BorderSubtle else Color.Transparent)
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(
                        start = 20.dp,
                        top = if (header != null) (spacing - 8.dp).coerceAtLeast(0.dp) else 6.dp,
                        end = 20.dp,
                        bottom = bottomPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}
