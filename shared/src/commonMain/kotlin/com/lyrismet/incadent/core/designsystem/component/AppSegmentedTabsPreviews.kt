package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
private fun AppSegmentedTabsPreview() {
    AppTheme {
        Column(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppSegmentedTabs(
                tabs =
                    listOf(
                        SegmentedTab("all", "Все", count = 12),
                        SegmentedTab("party", "Отряд", count = 4),
                        SegmentedTab("npc", "NPC", count = 8),
                    ),
                selected = "all",
                onSelected = {},
            )
            AppSegmentedTabs(
                tabs = listOf(SegmentedTab("roman", "Римские"), SegmentedTab("arabic", "Арабские")),
                selected = "arabic",
                onSelected = {},
            )
        }
    }
}
