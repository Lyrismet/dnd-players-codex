package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.campaign.CampaignSheetEvent
import com.lyrismet.incadent.core.campaign.CampaignSheetState
import com.lyrismet.incadent.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.campaign_create_name_label
import dndplayerscodex.shared.generated.resources.campaign_delete_body
import dndplayerscodex.shared.generated.resources.campaign_delete_title
import dndplayerscodex.shared.generated.resources.campaign_finish_archive
import dndplayerscodex.shared.generated.resources.campaign_finish_body
import dndplayerscodex.shared.generated.resources.campaign_finish_delete
import dndplayerscodex.shared.generated.resources.campaign_finish_title
import org.jetbrains.compose.resources.stringResource

// the calm confirmation of the mockup: archive by default, deleting the data is the quiet way out below it
@Composable
internal fun CampaignFinishDialog(
    sheet: CampaignSheetState.Finish,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    AppDialog(onDismissRequest = {
        onEvent(CampaignSheetEvent.Dismissed)
    }, borderColor = AppPalette.Gold.copy(alpha = 0.5f)) {
        Text(
            text = stringResource(Res.string.campaign_finish_title, sheet.name),
            style = MaterialTheme.typography.headlineMedium,
            color = AppPalette.TextHeading,
        )
        DialogBody(stringResource(Res.string.campaign_finish_body))
        FormPrimaryButton(
            text = stringResource(Res.string.campaign_finish_archive),
            onClick = { onEvent(CampaignSheetEvent.ArchiveConfirmed) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogTextAction(
                text = stringResource(Res.string.action_cancel),
                color = AppPalette.TextPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(CampaignSheetEvent.Dismissed) },
            )
            DialogTextAction(
                text = stringResource(Res.string.campaign_finish_delete),
                color = AppPalette.MaroonBright,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(CampaignSheetEvent.DeleteDataClicked) },
            )
        }
    }
}

@Composable
internal fun CampaignDeleteDialog(
    sheet: CampaignSheetState.DeleteData,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    AppDialog(onDismissRequest = {
        onEvent(CampaignSheetEvent.Dismissed)
    }, borderColor = AppPalette.Maroon.copy(alpha = 0.7f)) {
        Text(
            text = stringResource(Res.string.campaign_delete_title, sheet.name),
            style = MaterialTheme.typography.headlineMedium,
            color = AppPalette.TextHeading,
        )
        DialogBody(stringResource(Res.string.campaign_delete_body))
        FormTextField(
            label = stringResource(Res.string.campaign_create_name_label),
            value = sheet.typed,
            onChange = { onEvent(CampaignSheetEvent.DeleteTypedChanged(it)) },
            placeholder = sheet.name,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogTextAction(
                text = stringResource(Res.string.action_cancel),
                color = AppPalette.TextPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(CampaignSheetEvent.Dismissed) },
            )
            DialogTextAction(
                text = stringResource(Res.string.action_delete),
                color = if (sheet.canDelete) AppPalette.MaroonBright else AppPalette.TextTertiary,
                modifier = Modifier.weight(1f),
                onClick = { if (sheet.canDelete) onEvent(CampaignSheetEvent.DeleteConfirmed) },
            )
        }
    }
}

@Composable
private fun DialogTextAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .appCard(RoundedCornerShape(12.dp), Color.Transparent, AppPalette.BorderHover, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun DialogBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        color = AppPalette.TextMuted,
    )
}
