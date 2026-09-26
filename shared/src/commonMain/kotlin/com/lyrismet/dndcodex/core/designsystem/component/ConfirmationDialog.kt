package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.action_delete
import org.jetbrains.compose.resources.stringResource

/**
 * "are you sure?" dialog for any destructive action (delete an entry, remove an NPC...) -
 * call it from a screen's own pending-confirmation ui state, never trigger the action directly
 */
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(Res.string.action_delete),
    dismissLabel: String = stringResource(Res.string.action_cancel),
    confirmColor: Color = AppPalette.MaroonBright,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
        modifier = modifier,
    )
}
