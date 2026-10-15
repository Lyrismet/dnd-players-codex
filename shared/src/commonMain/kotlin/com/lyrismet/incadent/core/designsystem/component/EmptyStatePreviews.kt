package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun EmptyStateOrnamentPreview() {
    AppTheme {
        EmptyState(
            title = "Пока пусто",
            text = "Записи появятся здесь, когда начнётся игра",
            ornament = true,
            modifier = Modifier.height(280.dp).background(AppPalette.Background),
        )
    }
}

@Preview
@Composable
private fun EmptyStateActionsPreview() {
    AppTheme {
        EmptyState(
            title = "Нет сессий",
            text = "Создайте первую сессию",
            primaryAction = EmptyStateAction("Новая сессия") {},
            modifier = Modifier.height(280.dp).background(AppPalette.Background),
        )
    }
}
