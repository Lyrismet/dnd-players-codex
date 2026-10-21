package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

private const val PREVIEW_COUNT = 5
private val PreviewColor = Color(0xFFFF8A3D)
private const val PREVIEW_ICON =
    "M12 22c-4 0-7-2.8-7-6.8 0-2.6 1.2-4.4 2.6-5.8.2 1.6 1 2.8 2." +
        "2 3.3-.4-3.6 1-7.2 4.2-10.2-.3 3 1 4.8 2.6 6.5.4-1 .4-2 .1-3.2C19 8 19 11 19 15.2 19 19.2 16 22 12 22z"

@Preview
@Composable
private fun AppDamageShieldPreview() {
    AppTheme {
        Row(
            modifier = Modifier.background(AppPalette.Background).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShieldStyle.entries.forEach { style ->
                AppDamageShield(PREVIEW_ICON, PreviewColor, style)
            }
            AppDamageShield(PREVIEW_ICON, PreviewColor, ShieldStyle.Resist, count = PREVIEW_COUNT)
        }
    }
}
