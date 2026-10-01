package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.icons.AppIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.action_delete
import org.jetbrains.compose.resources.stringResource

/**
 * "are you sure?" dialog for any destructive action (delete an entry, remove an NPC...) -
 * call it from a screen's own pending-confirmation ui state, never trigger the action directly.
 * Built on the generic [AppDialog] shell with a maroon border, matching the mockup's `hasConfirm` card.
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
    confirmColor: Color = AppPalette.Maroon,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        borderColor = AppPalette.Maroon.copy(alpha = 0.7f),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppPalette.Maroon.copy(alpha = 0.2f))
                    .border(1.dp, AppPalette.Maroon.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Delete, contentDescription = null, tint = AppPalette.MaroonBright)
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
                Text(confirmLabel, color = Color.White, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
