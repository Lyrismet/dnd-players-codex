package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** the plain 1dp rule separating a header/footer from the content beside it - full-width by default */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier.fillMaxWidth(),
    color: Color = AppPalette.BorderSubtle,
    thickness: Dp = 1.dp,
) {
    Box(modifier.height(thickness).background(color))
}
