package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun AppTextInputPreview() {
    AppTheme {
        Column(
            modifier = Modifier.background(AppPalette.Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextInput(
                value = "",
                onValueChange = {},
                placeholder = "Название",
                style = AppTextInputStyle(height = 48.dp),
            )
            AppTextInput(
                value = "Рагнар Каменный",
                onValueChange = {},
                style = AppTextInputStyle(height = 48.dp),
                counter = AppTextInputCounter(max = 32),
            )
            AppTextInput(
                value = "Кузнец, который знает больше, чем говорит.",
                onValueChange = {},
                singleLine = false,
                minLines = 3,
                counter = AppTextInputCounter(max = 2000),
                style =
                    AppTextInputStyle(
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(14.dp, 12.dp),
                    ),
                modifier = Modifier.height(96.dp),
            )
        }
    }
}
