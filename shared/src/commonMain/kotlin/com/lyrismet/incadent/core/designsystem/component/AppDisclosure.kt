package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** a dashed "more" card with a grey [summary] while closed, the caller owns the expanded flag */
@Composable
fun AppDisclosure(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onToggle)
                    .dashedBorder(AppPalette.BorderHover, cornerRadius = 12.dp)
                    .defaultMinSize(minHeight = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AppPalette.GoldBright)
                if (!expanded) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                if (expanded) "▴" else "▾",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextSecondary,
            )
        }
        if (expanded) content()
    }
}
