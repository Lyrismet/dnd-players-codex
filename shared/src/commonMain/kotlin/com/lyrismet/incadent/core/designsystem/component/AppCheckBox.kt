package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

private const val CARRIED_FILL_ALPHA = 0.3f

/** the square tick box of a list, an [accent] fill with a dark tick or an empty ring, [carried] is a faded fill */
@Composable
fun AppCheckBox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = AppPalette.Gold,
    carried: Boolean = false,
) {
    val shape = AppShapes.Badge
    val fill =
        when {
            carried -> accent.copy(alpha = CARRIED_FILL_ALPHA)
            checked -> accent
            else -> null
        }
    Box(
        modifier =
            modifier
                .size(22.dp)
                .clip(shape)
                .background(fill ?: Color.Transparent)
                .border(1.5.dp, fill ?: AppPalette.RadioIdle, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                text = "✓",
                color = AppPalette.Background,
                fontSize = AppFontSize.Body,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
