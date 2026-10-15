package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.SheetDraftInput
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.session.domain.tags.sessionTagLabel
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_tag_add_all_button
import dndplayerscodex.shared.generated.resources.session_detail_tag_add_suggestion_button
import dndplayerscodex.shared.generated.resources.session_detail_tag_catalog_header
import dndplayerscodex.shared.generated.resources.session_detail_tag_custom_header
import dndplayerscodex.shared.generated.resources.session_detail_tag_disclaimer
import dndplayerscodex.shared.generated.resources.session_detail_tag_draft_placeholder
import dndplayerscodex.shared.generated.resources.session_detail_tag_sheet_title
import dndplayerscodex.shared.generated.resources.session_detail_tag_suggested_header
import org.jetbrains.compose.resources.stringResource

/** the "Теги сессии" bottom sheet (Players Codex v6.dc.html:787-810) - catalog toggle, auto-suggestions, custom tag */
@Composable
internal fun SessionDetailTagsSheet(
    sheet: SessionTagSheetState,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { eventSink(SessionDetailEvent.TagSheetDismissed) }) {
        AppSheetColumn(spacing = 18.dp) {
            AppSheetHeader(
                title = stringResource(Res.string.session_detail_tag_sheet_title),
                onClose = { eventSink(SessionDetailEvent.TagSheetDismissed) },
                overline = sheet.overline.ifEmpty { null },
                subtitle = sheet.sessionTitle,
            )
            if (sheet.suggestions.isNotEmpty()) {
                TagSuggestionsSection(sheet.suggestions, eventSink)
            }
            TagCatalogSection(sheet.catalog, eventSink)
            TagCustomInputSection(sheet.draft, eventSink)
            Text(
                stringResource(Res.string.session_detail_tag_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
            )
        }
    }
}

@Composable
private fun TagSuggestionsSection(
    suggestions: List<SessionTagSuggestionItem>,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormFieldLabel(
                text = stringResource(Res.string.session_detail_tag_suggested_header),
                color = AppPalette.GoldBright,
            )
            AppButton(
                text = stringResource(Res.string.session_detail_tag_add_all_button),
                onClick = { eventSink(SessionDetailEvent.TagAddAllSuggestionsClicked) },
            )
        }
        suggestions.forEach { suggestion ->
            TagSuggestionRow(suggestion, onAdd = { eventSink(SessionDetailEvent.TagSuggestionAdded(suggestion.tag)) })
        }
    }
}

@Composable
private fun TagSuggestionRow(
    suggestion: SessionTagSuggestionItem,
    onAdd: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Gold.copy(alpha = 0.06f),
                    border = AppPalette.Gold.copy(alpha = 0.3f),
                ).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sessionTagLabel(suggestion.tag),
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextHeading,
            )
            Text(
                suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AppButton(
            text = stringResource(Res.string.session_detail_tag_add_suggestion_button),
            onClick = onAdd,
            variant = ButtonVariant.Solid,
        )
    }
}

@Composable
private fun TagCatalogSection(
    catalog: List<SessionTagCatalogItem>,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = stringResource(Res.string.session_detail_tag_catalog_header),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            catalog.forEach { item ->
                SelectablePill(
                    text = sessionTagLabel(item.name).let { if (item.assigned) "✓ $it" else it },
                    selected = item.assigned,
                    onClick = { eventSink(SessionDetailEvent.TagToggled(item.name)) },
                )
            }
        }
    }
}

@Composable
private fun TagCustomInputSection(
    draft: String,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(
            text = stringResource(Res.string.session_detail_tag_custom_header),
        )
        SheetDraftInput(
            draft = draft,
            placeholder = stringResource(Res.string.session_detail_tag_draft_placeholder),
            onDraftChanged = { eventSink(SessionDetailEvent.TagDraftChanged(it)) },
            onSubmit = { eventSink(SessionDetailEvent.TagDraftSubmitted) },
            maxLength = TextLimits.TAG,
        )
    }
}
