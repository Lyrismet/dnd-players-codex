package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/**
 * small rounded label pill - multi-day session badges, generic tags, "has mentions" indicators, status badges.
 * [height], when given, centers the text in a fixed-height box instead of sizing from vertical padding - for a
 * row where the chip must align with a sibling of a fixed height (e.g. the session-detail header's tag row).
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    foreground: Color = AppPalette.TextPrimary,
    background: Color = AppPalette.BorderSubtle,
    border: Color? = null,
    height: Dp? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    val chrome =
        modifier
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .padding(
                horizontal = 8.dp,
                vertical =
                    when {
                        height != null -> 0.dp
                        border != null -> 3.dp
                        else -> 2.dp
                    },
            )
    if (height != null) {
        Box(modifier = chrome, contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = foreground)
        }
    } else {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = foreground, modifier = chrome)
    }
}
