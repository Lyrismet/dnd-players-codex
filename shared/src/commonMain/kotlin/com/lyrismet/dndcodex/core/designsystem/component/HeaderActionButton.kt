package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** the small gold-tinted pill button in a screen header, e.g. "+ Сессия" or "+ Запись" */
@Composable
fun HeaderActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = ButtonDefaults.ContentPadding,
        border = BorderStroke(1.dp, AppPalette.Gold.copy(alpha = 0.4f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = AppPalette.Gold.copy(alpha = 0.12f),
                contentColor = AppPalette.GoldBright,
            ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
