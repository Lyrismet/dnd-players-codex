package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** a titled switch card, with one line under the title when there is a [hint] */
@Composable
fun AppToggleCard(
    title: String,
    hint: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppPalette.TextPrimary,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = AppFontSize.Medium,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            if (hint != null) {
                Text(
                    text = hint,
                    modifier = Modifier.padding(top = 1.dp),
                    color = AppPalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
                )
            }
        }
        AppSwitch(checked = checked, onCheckedChange = { onToggle() })
    }
}
