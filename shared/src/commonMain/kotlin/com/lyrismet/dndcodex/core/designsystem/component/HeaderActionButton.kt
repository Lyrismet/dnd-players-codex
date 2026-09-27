package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

// the design's header pills are a subtle 10dp rounded rect, not Material3's default full-pill button shape
private val HeaderActionButtonShape = RoundedCornerShape(10.dp)

/** small tinted rounded-rect button in a screen header, e.g. "+ Сессия" (gold) or "Завершить" (maroon) */
@Composable
fun HeaderActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foreground: Color = AppPalette.GoldBright,
    background: Color = AppPalette.Gold.copy(alpha = 0.12f),
    border: Color = AppPalette.Gold.copy(alpha = 0.4f),
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = HeaderActionButtonShape,
        contentPadding = PaddingValues(horizontal = 12.dp),
        border = BorderStroke(1.dp, border),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = background,
                contentColor = foreground,
            ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
