package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun AppButtonTonesPreview() {
    AppTheme {
        Column(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ButtonTone.entries.forEach { tone ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonVariant.entries.forEach { variant ->
                        AppButton(text = "$tone", onClick = {}, tone = tone, variant = variant)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppButtonSizesPreview() {
    AppTheme {
        Column(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppButton(text = "Compact", onClick = {}, size = ButtonSize.Compact)
            AppButton(text = "Medium", onClick = {}, size = ButtonSize.Medium)
            AppButton(text = "Large", onClick = {}, size = ButtonSize.Large, tone = ButtonTone.Neutral)
            AppButton(
                text = "Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
            )
            AppButton(
                text = "Primary disabled",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
                enabled = false,
            )
        }
    }
}
