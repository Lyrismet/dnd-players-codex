package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AppListRowPreview() {
    AppListCard {
        AppListRow(title = "Все кампании", onClick = {})
        AppDivider()
        AppListRow(title = "Язык", subtitle = "Язык интерфейса", value = "Русский", onClick = {})
        AppDivider()
        AppListRow(title = "Версия", value = "1.0", valueWeight = FontWeight.Normal)
    }
}
