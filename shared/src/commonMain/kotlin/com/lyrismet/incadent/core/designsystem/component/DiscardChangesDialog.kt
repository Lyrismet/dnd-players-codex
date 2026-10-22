package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.discard_back
import dndplayerscodex.shared.generated.resources.discard_leave
import dndplayerscodex.shared.generated.resources.discard_title
import org.jetbrains.compose.resources.stringResource

/** "leave without saving?" over a sheet with unsaved input - going back is the gold button, leaving the plain one */
@Composable
fun DiscardChangesDialog(
    text: String,
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(Res.string.discard_title),
        text = text,
        onConfirm = onStay,
        onDismiss = onLeave,
        confirmLabel = stringResource(Res.string.discard_back),
        dismissLabel = stringResource(Res.string.discard_leave),
        confirmTone = ButtonTone.Gold,
        icon = AppIcons.Edit,
        accent = AppPalette.Gold,
        accentBright = AppPalette.GoldBright,
        onDismissRequest = onStay,
    )
}
