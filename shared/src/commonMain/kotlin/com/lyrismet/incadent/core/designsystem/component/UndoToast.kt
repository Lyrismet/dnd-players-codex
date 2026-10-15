package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.undo.UndoAction
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_undo
import org.jetbrains.compose.resources.stringResource

// caller keeps this composed always and just toggles action to null/non-null
@Composable
fun UndoToast(
    action: UndoAction?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBar(
        value = action,
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
    ) { current ->
        UndoToastContent(current, onUndo)
    }
}

@Composable
private fun UndoToastContent(
    action: UndoAction,
    onUndo: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            size = 32.dp,
            shape = AppShapes.Tile,
            background = AppPalette.Maroon.copy(alpha = 0.25f),
        ) {
            Icon(AppIcons.Delete, contentDescription = null, tint = AppPalette.MaroonBright)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(action.title, style = MaterialTheme.typography.titleSmall, color = AppPalette.TextHeading)
            Text(
                action.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onUndo, modifier = Modifier.height(44.dp)) {
            Text(
                stringResource(Res.string.action_undo),
                color = AppPalette.GoldBright,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
