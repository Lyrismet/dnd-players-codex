package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDialog
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
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
            AppButton(
                text = stringResource(Res.string.action_cancel),
                onClick = { onEvent(CampaignSheetEvent.Dismissed) },
                modifier = Modifier.weight(1f),
                tone = ButtonTone.Neutral,
                size = ButtonSize.Large,
                contentColor = AppPalette.TextPrimary,
            )
            AppButton(
                text = stringResource(Res.string.campaign_finish_delete),
                onClick = { onEvent(CampaignSheetEvent.DeleteDataClicked) },
                modifier = Modifier.weight(1f),
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
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
            AppButton(
                text = stringResource(Res.string.action_cancel),
                onClick = { onEvent(CampaignSheetEvent.Dismissed) },
                modifier = Modifier.weight(1f),
                tone = ButtonTone.Neutral,
                size = ButtonSize.Large,
                contentColor = AppPalette.TextPrimary,
            )
            AppButton(
                text = stringResource(Res.string.action_delete),
                onClick = { onEvent(CampaignSheetEvent.DeleteConfirmed) },
                modifier = Modifier.weight(1f),
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
                enabled = sheet.canDelete,
            )
        }
    }
}

@Composable
private fun DialogBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium, lineHeight = 21.sp),
        color = AppPalette.TextMuted,
    )
}
