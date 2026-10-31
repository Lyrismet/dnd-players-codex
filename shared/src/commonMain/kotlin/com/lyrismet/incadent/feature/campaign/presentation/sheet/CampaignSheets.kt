package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SelectCreateRow
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSections
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_import_from_file
import dndplayerscodex.shared.generated.resources.campaign_new
import dndplayerscodex.shared.generated.resources.campaign_sheet_title
import org.jetbrains.compose.resources.stringResource

/** draws whichever campaign sheet or dialog the host is in - the host owns every transition, this only reports taps */
@Composable
fun CampaignSheets(
    sheet: CampaignSheetState,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    when (sheet) {
        CampaignSheetState.Hidden -> Unit
        is CampaignSheetState.Library -> CampaignLibrarySheet(sheet, onEvent)
        is CampaignSheetState.Create -> CampaignCreateSheet(sheet, onEvent)
        is CampaignSheetState.Rename ->
            CampaignRenameSheet(
                draft = sheet.draft,
                onDraftChanged = { onEvent(CampaignSheetEvent.RenameDraftChanged(it)) },
                onSave = { onEvent(CampaignSheetEvent.RenameSaved) },
                onDismiss = { onEvent(CampaignSheetEvent.Dismissed) },
            )
        is CampaignSheetState.Finish -> CampaignFinishDialog(sheet, onEvent)
        is CampaignSheetState.DeleteData -> CampaignDeleteDialog(sheet, onEvent)
    }
}

@Composable
private fun CampaignLibrarySheet(
    sheet: CampaignSheetState.Library,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(CampaignSheetEvent.Dismissed) }) {
        AppSheetColumn(spacing = 10.dp, scroll = true, header = {
            AppSheetHeader(
                title = stringResource(Res.string.campaign_sheet_title),
                onClose = { onEvent(CampaignSheetEvent.Dismissed) },
            )
        }) {
            CampaignSections(
                library = sheet.library,
                onOpen = { onEvent(CampaignSheetEvent.CampaignClicked(it)) },
                onRename = { onEvent(CampaignSheetEvent.RenameClicked) },
                onFinish = { onEvent(CampaignSheetEvent.FinishClicked) },
                onExport = { onEvent(CampaignSheetEvent.ExportClicked(it)) },
            )
            SelectCreateRow(
                text = stringResource(Res.string.campaign_new),
                onClick = { onEvent(CampaignSheetEvent.NewCampaignClicked) },
            )
            AppButton(
                text = stringResource(Res.string.campaign_import_from_file),
                onClick = rememberImportPicker(onEvent),
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.Neutral,
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
            )
        }
    }
}
