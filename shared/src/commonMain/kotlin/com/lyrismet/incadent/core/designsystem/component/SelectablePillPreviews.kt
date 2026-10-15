package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun SelectablePillPreview() {
    AppTheme {
        Row(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SelectablePill("Все", selected = true, onClick = {}, size = PillSize.Small)
            SelectablePill("Друг", selected = false, onClick = {}, size = PillSize.Small)
            SelectablePill("Враг", selected = true, onClick = {}, selectedFill = PillFill.GoldTint)
            SelectablePill("✓ Не указано", selected = true, onClick = {}, selectedFill = PillFill.Neutral)
            SelectablePill("Бой", selected = false, onClick = {})
        }
    }
}
