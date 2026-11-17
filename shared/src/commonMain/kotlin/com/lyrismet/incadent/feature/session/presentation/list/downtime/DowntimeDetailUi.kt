package com.lyrismet.incadent.feature.session.presentation.list.downtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.downtime_apply
import incadent.shared.generated.resources.downtime_delete
import incadent.shared.generated.resources.downtime_detail_empty
import incadent.shared.generated.resources.downtime_detail_subtitle
import incadent.shared.generated.resources.downtime_edit
import incadent.shared.generated.resources.downtime_field_did
import incadent.shared.generated.resources.downtime_field_gained
import incadent.shared.generated.resources.downtime_field_long
import incadent.shared.generated.resources.downtime_loot_header
import incadent.shared.generated.resources.downtime_revert
import incadent.shared.generated.resources.downtime_status_no_character
import incadent.shared.generated.resources.downtime_title
import org.jetbrains.compose.resources.stringResource

private val ActionHeight = 52.dp

@Composable
internal fun DowntimeDetailSheet(
    state: DowntimeSheetState.Detail,
    onEvent: (DowntimeEvent) -> Unit,
) {
    val card = state.card
    val dismiss = { onEvent(DowntimeEvent.Dismissed) }
    AppBottomSheet(onDismissRequest = dismiss) {
        AppSheetColumn(spacing = 16.dp, scroll = true, header = {
            AppSheetHeader(
                overline = gapText(card.afterLabel, card.nextLabel).uppercase(),
                overlineColor = AppPalette.Parchment,
                title = stringResource(Res.string.downtime_title),
                subtitle = stringResource(Res.string.downtime_detail_subtitle),
                onClose = dismiss,
            )
        }) {
            DetailBody(card)
            if (card.hasLoot) LootBlock(state, onEvent)
            if (!state.readOnly) DetailActions(onEvent)
        }
    }
}

@Composable
private fun DetailBody(card: DowntimeCard) {
    if (card.isEmpty) {
        Text(
            stringResource(Res.string.downtime_detail_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = AppPalette.TextSecondary,
        )
    }
    if (card.shortText.isNotBlank()) {
        DetailBlock(stringResource(Res.string.downtime_field_did)) {
            Text(card.shortText, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextHeading)
        }
    }
    if (card.longText.isNotBlank()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(AppPalette.Parchment.copy(alpha = 0.35f)))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormFieldLabel(text = stringResource(Res.string.downtime_field_long))
                card.longText.split('\n').filter { it.isNotBlank() }.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextDescription)
                }
            }
        }
    }
    if (card.gainedText.isNotBlank()) {
        DetailBlock(stringResource(Res.string.downtime_field_gained)) {
            Text(card.gainedText, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextDescription)
        }
    }
}

@Composable
private fun DetailBlock(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(text = label)
        content()
    }
}

/** the gold and things with where they stand, and the button that puts them in the sheet or takes them out */
@Composable
private fun LootBlock(
    state: DowntimeSheetState.Detail,
    onEvent: (DowntimeEvent) -> Unit,
) {
    val card = state.card
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(text = stringResource(Res.string.downtime_loot_header))
        LootChips(card)
        if (!state.readOnly && !state.canApply && !card.applied) {
            Text(
                stringResource(Res.string.downtime_status_no_character),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextSecondary,
            )
        }
        if (!state.readOnly && state.canApply) {
            AppButton(
                text = stringResource(if (card.applied) Res.string.downtime_revert else Res.string.downtime_apply),
                onClick = { onEvent(if (card.applied) DowntimeEvent.RevertClicked else DowntimeEvent.ApplyClicked) },
                tone = if (card.applied) ButtonTone.Neutral else ButtonTone.Gold,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun DetailActions(onEvent: (DowntimeEvent) -> Unit) {
    DowntimeActionRow(
        primaryText = stringResource(Res.string.downtime_edit),
        onPrimary = { onEvent(DowntimeEvent.EditClicked) },
        onDelete = { onEvent(DowntimeEvent.DeleteClicked) },
    )
}

/** the primary button of a downtime window with the delete button before it, none when [onDelete] is null */
@Composable
internal fun DowntimeActionRow(
    primaryText: String,
    onPrimary: () -> Unit,
    onDelete: (() -> Unit)?,
    enabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (onDelete != null) {
            AppButton(
                text = stringResource(Res.string.downtime_delete),
                onClick = onDelete,
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Primary,
                height = ActionHeight,
            )
        }
        FormPrimaryButton(text = primaryText, onClick = onPrimary, modifier = Modifier.weight(1f), enabled = enabled)
    }
}
