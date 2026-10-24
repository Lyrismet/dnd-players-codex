package com.lyrismet.incadent.feature.phrases.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppRemoveButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.SheetDraftInput
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.phrases.presentation.PhraseFrequentRow
import com.lyrismet.incadent.feature.phrases.presentation.PhrasePinnedRow
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetEvent
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.phrase_sheet_disclaimer
import dndplayerscodex.shared.generated.resources.phrase_sheet_draft_placeholder
import dndplayerscodex.shared.generated.resources.phrase_sheet_frequent_empty
import dndplayerscodex.shared.generated.resources.phrase_sheet_frequent_header
import dndplayerscodex.shared.generated.resources.phrase_sheet_intro
import dndplayerscodex.shared.generated.resources.phrase_sheet_overline
import dndplayerscodex.shared.generated.resources.phrase_sheet_pin_button
import dndplayerscodex.shared.generated.resources.phrase_sheet_pinned_empty
import dndplayerscodex.shared.generated.resources.phrase_sheet_pinned_header
import dndplayerscodex.shared.generated.resources.phrase_sheet_title
import org.jetbrains.compose.resources.stringResource

/** the "Заготовки" bottom sheet (Players Codex v6.dc.html `isSnipSheet`) - pinned phrases, frequent ones, own input */
@Composable
fun PhraseSnippetsSheet(
    state: PhraseSheetState,
    onEvent: (PhraseSheetEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(PhraseSheetEvent.Dismissed) }) {
        AppSheetColumn(spacing = 18.dp, scroll = true) {
            AppSheetHeader(
                title = stringResource(Res.string.phrase_sheet_title),
                onClose = { onEvent(PhraseSheetEvent.Dismissed) },
                overline = stringResource(Res.string.phrase_sheet_overline),
                intro = stringResource(Res.string.phrase_sheet_intro),
            )
            PinnedPhrasesSection(state, onEvent)
            FrequentPhrasesSection(state.frequent, onEvent)
            Text(
                stringResource(Res.string.phrase_sheet_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
            )
        }
    }
}

@Composable
private fun PinnedPhrasesSection(
    state: PhraseSheetState,
    onEvent: (PhraseSheetEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = stringResource(Res.string.phrase_sheet_pinned_header),
            trailingContent = {
                Text(state.pinned.size.toString(), fontSize = AppFontSize.Small, color = AppPalette.GoldBright)
            },
        )
        state.pinned.forEach { row -> PinnedPhraseRow(row, onRemove = { onEvent(PhraseSheetEvent.Removed(row.text)) }) }
        if (state.pinned.isEmpty()) {
            Text(
                stringResource(Res.string.phrase_sheet_pinned_empty),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body),
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            )
        }
        SheetDraftInput(
            draft = state.draft,
            placeholder = stringResource(Res.string.phrase_sheet_draft_placeholder),
            onDraftChanged = { onEvent(PhraseSheetEvent.DraftChanged(it)) },
            onSubmit = { onEvent(PhraseSheetEvent.DraftSubmitted) },
        )
    }
}

@Composable
private fun PinnedPhraseRow(
    row: PhrasePinnedRow,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.BorderSubtle,
                ).padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.label,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = AppPalette.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        AppRemoveButton(onClick = onRemove)
    }
}

@Composable
private fun FrequentPhrasesSection(
    frequent: List<PhraseFrequentRow>,
    onEvent: (PhraseSheetEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = stringResource(Res.string.phrase_sheet_frequent_header),
            color = AppPalette.Azure,
        )
        frequent.forEach { row -> FrequentPhraseRow(row, onPin = { onEvent(PhraseSheetEvent.Pinned(row.text)) }) }
        if (frequent.isEmpty()) {
            Text(
                stringResource(Res.string.phrase_sheet_frequent_empty),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Body),
                color = AppPalette.TextTertiary,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FrequentPhraseRow(
    row: PhraseFrequentRow,
    onPin: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.BorderSubtle,
                ).padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.label,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = AppPalette.TextPrimary,
            )
            Text(
                row.meta,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus),
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        AppButton(
            text = stringResource(Res.string.phrase_sheet_pin_button),
            onClick = onPin,
            variant = ButtonVariant.Ghost,
        )
    }
}
