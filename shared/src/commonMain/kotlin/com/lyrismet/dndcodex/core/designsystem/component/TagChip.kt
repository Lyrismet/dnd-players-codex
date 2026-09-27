package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** small rounded label pill - multi-day session badges, generic tags, "has mentions" indicators, status badges */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    foreground: Color = AppPalette.TextPrimary,
    background: Color = AppPalette.BorderSubtle,
    border: Color? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier =
            modifier
                .clip(shape)
                .background(background)
                .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
                .padding(horizontal = 8.dp, vertical = if (border != null) 3.dp else 2.dp),
    )
}
