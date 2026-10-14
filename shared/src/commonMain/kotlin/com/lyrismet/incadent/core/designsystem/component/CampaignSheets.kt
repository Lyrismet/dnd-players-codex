package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.campaign.CampaignSheetEvent
import com.lyrismet.incadent.core.campaign.CampaignSheetState
import dndplayerscodex.shared.generated.resources.Res
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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppSheetHeader(
                title = stringResource(Res.string.campaign_sheet_title),
                onClose = { onEvent(CampaignSheetEvent.Dismissed) },
            )
            CampaignSections(
                library = sheet.library,
                onOpen = { onEvent(CampaignSheetEvent.CampaignClicked(it)) },
                onRename = { onEvent(CampaignSheetEvent.RenameClicked) },
                onFinish = { onEvent(CampaignSheetEvent.FinishClicked) },
            )
            SelectCreateRow(
                text = stringResource(Res.string.campaign_new),
                onClick = { onEvent(CampaignSheetEvent.NewCampaignClicked) },
            )
        }
    }
}
