package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppTheme

@Preview
@Composable
private fun DashedBorderPreview() {
    AppTheme {
        Box(modifier = Modifier.background(AppPalette.Background).padding(16.dp)) {
            Box(Modifier.size(width = 160.dp, height = 48.dp).dashedBorder(AppPalette.Gold, cornerRadius = 12.dp))
        }
    }
}
