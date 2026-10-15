package com.lyrismet.incadent.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppListRow

// the gesture-hint row of the diary group - "Показать" re-arms the one-time card in the codex
@Composable
internal fun HintsRow(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    AppListRow(title = title, subtitle = description) {
        AppButton(text = actionLabel, onClick = onAction)
    }
}

// the "Заготовки фраз" row of the diary group - the summary counts pinned and found phrases, a tap opens the sheet
@Composable
internal fun PhrasesRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    AppListRow(title = title, subtitle = summary, onClick = onClick)
}
