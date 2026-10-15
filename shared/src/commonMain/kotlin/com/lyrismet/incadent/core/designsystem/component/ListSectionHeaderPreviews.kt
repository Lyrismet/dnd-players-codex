package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun ListSectionHeaderPreview() {
    AppTheme {
        Column(modifier = Modifier.background(AppPalette.Background).padding(16.dp)) {
            ListSectionHeader(text = "Главные NPC", count = 4)
            ListSectionHeader(text = "Архив")
        }
    }
}
