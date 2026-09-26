package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** small rounded label pill - multi-day session badges, generic tags, "has mentions" indicators */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    foreground: Color = AppPalette.TextPrimary,
    background: Color = AppPalette.BorderSubtle,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
