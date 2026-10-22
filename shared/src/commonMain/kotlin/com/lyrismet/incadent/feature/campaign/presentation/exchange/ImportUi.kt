package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.DiscardChangesDialog
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.RadioOptionRow
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.file.rememberFilePicker
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_dest_add
import dndplayerscodex.shared.generated.resources.exchange_dest_add_hint
import dndplayerscodex.shared.generated.resources.exchange_dest_label
import dndplayerscodex.shared.generated.resources.exchange_dest_new
import dndplayerscodex.shared.generated.resources.exchange_dest_new_hint
import dndplayerscodex.shared.generated.resources.exchange_discard_text
import dndplayerscodex.shared.generated.resources.exchange_error_title
import dndplayerscodex.shared.generated.resources.exchange_import_campaign_title
import dndplayerscodex.shared.generated.resources.exchange_import_campaign_untitled
import dndplayerscodex.shared.generated.resources.exchange_import_file_line
import dndplayerscodex.shared.generated.resources.exchange_import_go
import dndplayerscodex.shared.generated.resources.exchange_import_info
import dndplayerscodex.shared.generated.resources.exchange_import_overline
import dndplayerscodex.shared.generated.resources.exchange_import_pick_title
import dndplayerscodex.shared.generated.resources.exchange_import_reading
import dndplayerscodex.shared.generated.resources.exchange_import_with_photos
import dndplayerscodex.shared.generated.resources.exchange_import_without_photos
import dndplayerscodex.shared.generated.resources.exchange_items_from
import dndplayerscodex.shared.generated.resources.exchange_items_title
import dndplayerscodex.shared.generated.resources.exchange_match_title
import dndplayerscodex.shared.generated.resources.exchange_next
import dndplayerscodex.shared.generated.resources.exchange_next_format
import dndplayerscodex.shared.generated.resources.exchange_records_count
import dndplayerscodex.shared.generated.resources.exchange_sessions_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** shows the import as a bottom sheet over the app, one step at a time, each with a way back or out */
@Composable
fun ImportUi(
    state: ImportState,
    modifier: Modifier = Modifier,
) {
    val sink = state.eventSink
    val pickFile = rememberFilePicker { sink(ImportEvent.FilePicked(it)) }
    AppBottomSheet(
        onDismissRequest = { sink(ImportEvent.CloseRequested) },
        modifier = modifier,
        // the sheet stays up under the prompt instead of hiding first
        onDismissBlocked = if (state.guardsClosing) ({ sink(ImportEvent.CloseRequested) }) else null,
    ) {
        AppSheetColumn(spacing = 12.dp, scroll = true) {
            AppSheetHeader(
                overline = stringResource(Res.string.exchange_import_overline),
                title = stepTitle(state),
                onClose = { sink(ImportEvent.CloseRequested) },
            )
            when (val step = state.step) {
                ImportStep.Reading, ImportStep.Working -> Note(stringResource(Res.string.exchange_import_reading))
                is ImportStep.Failed -> FailedStep(step.failure, onOtherFile = pickFile)
                ImportStep.Preview -> PreviewStep(state)
                ImportStep.Items -> ItemsStep(state)
                ImportStep.Match -> MatchStep(state)
                is ImportStep.Done -> DoneStep(step.records) { sink(ImportEvent.Closed) }
            }
        }
    }
    if (state.leavePromptVisible) {
        DiscardChangesDialog(
            text = stringResource(Res.string.exchange_discard_text),
            onStay = { sink(ImportEvent.LeaveCancelled) },
            onLeave = { sink(ImportEvent.Closed) },
        )
    }
}

@Composable
private fun stepTitle(state: ImportState): String =
    when (state.step) {
        ImportStep.Reading, ImportStep.Working -> stringResource(Res.string.exchange_import_pick_title)
        is ImportStep.Failed -> stringResource(Res.string.exchange_error_title)
        ImportStep.Preview ->
            state.campaignTitle.takeIf { it.isNotBlank() }?.let {
                stringResource(
                    Res.string.exchange_import_campaign_title,
                    it,
                )
            }
                ?: stringResource(Res.string.exchange_import_campaign_untitled)
        ImportStep.Items -> stringResource(Res.string.exchange_items_title)
        ImportStep.Match -> stringResource(Res.string.exchange_match_title)
        is ImportStep.Done -> stringResource(Res.string.exchange_import_overline)
    }

@Composable
private fun PreviewStep(state: ImportState) {
    val sink = state.eventSink
    ImportInfoCard(state)
    FormFieldLabel(stringResource(Res.string.exchange_dest_label))
    RadioOptionRow(
        title = stringResource(Res.string.exchange_dest_new),
        description =
            stringResource(
                Res.string.exchange_dest_new_hint,
                state.campaignTitle.ifBlank {
                    stringResource(Res.string.exchange_import_campaign_untitled)
                },
            ),
        selected = state.destination == ImportDestination.NEW,
        onClick = { sink(ImportEvent.DestinationSelected(ImportDestination.NEW)) },
    )
    state.currentName?.let { name ->
        RadioOptionRow(
            title = stringResource(Res.string.exchange_dest_add),
            description = stringResource(Res.string.exchange_dest_add_hint, name),
            selected = state.destination == ImportDestination.CURRENT,
            onClick = { sink(ImportEvent.DestinationSelected(ImportDestination.CURRENT)) },
        )
    }
    FormPrimaryButton(
        text =
            stringResource(
                if (state.destination ==
                    ImportDestination.NEW
                ) {
                    Res.string.exchange_import_go
                } else {
                    Res.string.exchange_next
                },
            ),
        onClick = { sink(ImportEvent.PreviewNext) },
    )
}

@Composable
private fun ImportInfoCard(state: ImportState) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text =
                stringResource(
                    Res.string.exchange_import_info,
                    pluralStringResource(Res.plurals.exchange_records_count, state.recordCount, state.recordCount),
                    pluralStringResource(Res.plurals.exchange_sessions_count, state.sessionCount, state.sessionCount),
                    stringResource(
                        if (state.fileHasPhotos) {
                            Res.string.exchange_import_with_photos
                        } else {
                            Res.string.exchange_import_without_photos
                        },
                    ),
                ),
            color = AppPalette.TextHeading,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(Res.string.exchange_import_file_line, state.fileName, fileSizeText(state.fileBytes)),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ItemsStep(state: ImportState) {
    val sink = state.eventSink
    Text(
        text =
            stringResource(
                Res.string.exchange_items_from,
                state.campaignTitle.ifBlank {
                    state.fileName
                },
                state.currentName.orEmpty(),
            ),
        color = AppPalette.TextSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
    state.pick?.let { view -> ExchangePickSection(view, onEvent = { sink(ImportEvent.Pick(it)) }) }
    if (state.fileHasPhotos) {
        ExchangePhotosCard(photos = state.photos, estimate = null, onToggle = {
            sink(ImportEvent.PhotosToggled)
        })
    }
    FormPrimaryButton(
        text =
            stringResource(
                Res.string.exchange_next_format,
                pluralStringResource(Res.plurals.exchange_records_count, state.pickRecordCount, state.pickRecordCount),
            ),
        onClick = { sink(ImportEvent.ItemsNext) },
        enabled = state.canTakeSelection,
    )
    BackLink { sink(ImportEvent.Back) }
}
