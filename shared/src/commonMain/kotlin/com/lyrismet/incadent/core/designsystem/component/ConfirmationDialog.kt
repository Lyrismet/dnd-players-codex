package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.action_delete
import org.jetbrains.compose.resources.stringResource

/** "are you sure?" dialog - call it from pending-confirmation state, never trigger the action directly */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(Res.string.action_delete),
    dismissLabel: String = stringResource(Res.string.action_cancel),
    confirmTone: ButtonTone = ButtonTone.Maroon,
    icon: ImageVector = AppIcons.Delete,
    accent: Color = AppPalette.Maroon,
    accentBright: Color = AppPalette.MaroonBright,
    onDismissRequest: () -> Unit = onDismiss,
) {
    AppDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.7f),
    ) {
        IconBadge(
            size = 44.dp,
            shape = AppShapes.Field,
            background = accent.copy(alpha = 0.2f),
            border = accent.copy(alpha = 0.7f),
        ) {
            Icon(icon, contentDescription = null, tint = accentBright)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium, lineHeight = 21.sp),
            color = AppPalette.TextMuted,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            AppButton(
                text = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = ButtonTone.Neutral,
                size = ButtonSize.Large,
                contentColor = AppPalette.TextPrimary,
            )
            AppButton(
                text = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                tone = confirmTone,
                variant = ButtonVariant.Solid,
                size = ButtonSize.Large,
            )
        }
    }
}
