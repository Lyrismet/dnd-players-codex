package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

private const val DIAMOND_ROTATION_DEGREES = 45f

/** short gold line + rotated diamond + long border line - the ornamental rule under a screen title */
@Composable
fun HeaderDivider(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(28.dp).height(1.dp).background(AppPalette.Gold))
        Spacer(Modifier.width(8.dp))
        Spacer(Modifier.size(5.dp).rotate(DIAMOND_ROTATION_DEGREES).border(1.dp, AppPalette.Gold))
        Spacer(Modifier.width(8.dp))
        AppDivider(modifier = Modifier.weight(1f), color = AppPalette.Border)
    }
}
