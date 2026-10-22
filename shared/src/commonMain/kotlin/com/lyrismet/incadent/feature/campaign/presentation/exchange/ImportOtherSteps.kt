package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_back
import dndplayerscodex.shared.generated.resources.exchange_done
import dndplayerscodex.shared.generated.resources.exchange_error_broken_body
import dndplayerscodex.shared.generated.resources.exchange_error_broken_title
import dndplayerscodex.shared.generated.resources.exchange_error_large_body
import dndplayerscodex.shared.generated.resources.exchange_error_large_title
import dndplayerscodex.shared.generated.resources.exchange_error_newer_body
import dndplayerscodex.shared.generated.resources.exchange_error_newer_title
import dndplayerscodex.shared.generated.resources.exchange_error_not_incadent_body
import dndplayerscodex.shared.generated.resources.exchange_error_not_incadent_title
import dndplayerscodex.shared.generated.resources.exchange_error_other_file
import dndplayerscodex.shared.generated.resources.exchange_error_read_body
import dndplayerscodex.shared.generated.resources.exchange_error_read_title
import dndplayerscodex.shared.generated.resources.exchange_import_button_count
import dndplayerscodex.shared.generated.resources.exchange_import_done_toast
import dndplayerscodex.shared.generated.resources.exchange_kind_npc
import dndplayerscodex.shared.generated.resources.exchange_kind_party
import dndplayerscodex.shared.generated.resources.exchange_kind_place
import dndplayerscodex.shared.generated.resources.exchange_kind_quest
import dndplayerscodex.shared.generated.resources.exchange_kind_species
import dndplayerscodex.shared.generated.resources.exchange_match_copy
import dndplayerscodex.shared.generated.resources.exchange_match_copy_note
import dndplayerscodex.shared.generated.resources.exchange_match_counts
import dndplayerscodex.shared.generated.resources.exchange_match_label
import dndplayerscodex.shared.generated.resources.exchange_match_row_copy
import dndplayerscodex.shared.generated.resources.exchange_match_row_skip
import dndplayerscodex.shared.generated.resources.exchange_match_skip
import dndplayerscodex.shared.generated.resources.exchange_match_skip_note
import dndplayerscodex.shared.generated.resources.exchange_tab_sessions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Note(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        color = AppPalette.TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun FailedStep(
    failure: ImportFailure,
    onOtherFile: () -> Unit,
) {
    val (title, body) = failureTexts(failure)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Maroon.copy(alpha = 0.1f), AppPalette.Maroon.copy(alpha = 0.5f))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(title), color = AppPalette.MaroonBright, style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(body),
            color = AppPalette.TextDescription,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    FormPrimaryButton(text = stringResource(Res.string.exchange_error_other_file), onClick = onOtherFile)
}

private fun failureTexts(failure: ImportFailure): Pair<StringResource, StringResource> =
    when (failure) {
        ImportFailure.Unreadable -> Res.string.exchange_error_read_title to Res.string.exchange_error_read_body
        is ImportFailure.Rejected ->
            when (failure.error) {
                ImportError.NotIncadent ->
                    Res.string.exchange_error_not_incadent_title to Res.string.exchange_error_not_incadent_body
                ImportError.NewerVersion ->
                    Res.string.exchange_error_newer_title to
                        Res.string.exchange_error_newer_body
                ImportError.TooLarge -> Res.string.exchange_error_large_title to Res.string.exchange_error_large_body
                ImportError.Malformed -> Res.string.exchange_error_broken_title to Res.string.exchange_error_broken_body
            }
    }

@Composable
internal fun MatchStep(state: ImportState) {
    val sink = state.eventSink
    FormFieldLabel(stringResource(Res.string.exchange_match_label))
    Text(
        text = stringResource(Res.string.exchange_match_counts, state.newCount, state.duplicates.size),
        color = AppPalette.TextHeading,
        style = MaterialTheme.typography.titleSmall,
    )
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(ConflictMode.SKIP, stringResource(Res.string.exchange_match_skip)),
                SegmentedTab(ConflictMode.COPY, stringResource(Res.string.exchange_match_copy)),
            ),
        selected = state.conflicts,
        onSelected = { sink(ImportEvent.ConflictsSelected(it)) },
    )
    Column(
        modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.duplicates.forEach { duplicate -> DuplicateRow(duplicate, state.conflicts) }
    }
    Text(
        text =
            stringResource(
                if (state.conflicts ==
                    ConflictMode.SKIP
                ) {
                    Res.string.exchange_match_skip_note
                } else {
                    Res.string.exchange_match_copy_note
                },
            ),
        color = AppPalette.TextSecondary,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus),
    )
    FormPrimaryButton(
        text = stringResource(Res.string.exchange_import_button_count, state.importCount),
        onClick = { sink(ImportEvent.ImportConfirmed) },
    )
    BackLink { sink(ImportEvent.Back) }
}

@Composable
private fun DuplicateRow(
    duplicate: ImportDuplicate,
    mode: ConflictMode,
) {
    val kind = stringResource(duplicate.kind.label())
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Field, AppPalette.Background, AppPalette.BorderSubtle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = if (mode == ConflictMode.COPY) "${duplicate.name} (2)" else duplicate.name,
            color = AppPalette.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text =
                stringResource(
                    if (mode ==
                        ConflictMode.COPY
                    ) {
                        Res.string.exchange_match_row_copy
                    } else {
                        Res.string.exchange_match_row_skip
                    },
                    kind,
                ),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun ExchangeKind.label(): StringResource =
    when (this) {
        ExchangeKind.PLACE -> Res.string.exchange_kind_place
        ExchangeKind.NPC -> Res.string.exchange_kind_npc
        ExchangeKind.QUEST -> Res.string.exchange_kind_quest
        ExchangeKind.SPECIES -> Res.string.exchange_kind_species
        ExchangeKind.PARTY -> Res.string.exchange_kind_party
        ExchangeKind.SESSION -> Res.string.exchange_tab_sessions
    }

@Composable
internal fun DoneStep(
    records: Int,
    onDone: () -> Unit,
) {
    Text(
        text = pluralStringResource(Res.plurals.exchange_import_done_toast, records, records),
        color = AppPalette.EmeraldBright,
        style = MaterialTheme.typography.titleMedium,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        AppButton(
            text = stringResource(Res.string.exchange_done),
            onClick = onDone,
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Large,
        )
    }
}

@Composable
internal fun BackLink(onClick: () -> Unit) {
    Text(
        text = stringResource(Res.string.exchange_back),
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Control, Color.Transparent, null, onClick = onClick)
                .padding(vertical = 10.dp),
        color = AppPalette.TextMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
}
