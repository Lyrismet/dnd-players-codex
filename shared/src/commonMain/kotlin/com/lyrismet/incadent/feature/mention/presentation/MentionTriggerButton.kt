package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.IconBadge

/** the rounded "@" square next to a notes input, a tap types the "@" that opens the dropdown */
@Composable
fun MentionTriggerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    description: String? = null,
) {
    IconBadge(
        modifier = if (description == null) modifier else modifier.semantics { contentDescription = description },
        size = size,
        shape = AppShapes.Tile,
        background = AppPalette.SurfaceElevated,
        onClick = onClick,
    ) {
        Text(
            "@",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.Large),
            color = AppPalette.GoldBright,
        )
    }
}
