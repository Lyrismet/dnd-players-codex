package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
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
    confirmColor: Color = AppPalette.Maroon,
    confirmContentColor: Color = Color.White,
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
            shape = RoundedCornerShape(12.dp),
            background = accent.copy(alpha = 0.2f),
            border = accent.copy(alpha = 0.7f),
        ) {
            Icon(icon, contentDescription = null, tint = accentBright)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = AppPalette.TextMuted,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppPalette.BorderHover),
            ) {
                Text(dismissLabel, color = AppPalette.TextPrimary, style = MaterialTheme.typography.titleSmall)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
            ) {
                Text(confirmLabel, color = confirmContentColor, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
