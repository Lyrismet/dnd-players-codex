package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.appCard
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TagSheetHeader(sheet, onClose = { eventSink(SessionDetailEvent.TagSheetDismissed) })
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
private fun TagSheetHeader(
    sheet: SessionTagSheetState,
    onClose: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sheet.overline,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.16.em),
                color = AppPalette.Gold,
            )
            Text(
                stringResource(Res.string.session_detail_tag_sheet_title),
                style = MaterialTheme.typography.headlineLarge,
                color = AppPalette.TextHeading,
            )
            Text(
                sheet.sessionTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        SheetCloseButton(onClick = onClose, size = 40.dp)
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
            SectionOverline(
                text = stringResource(Res.string.session_detail_tag_suggested_header),
                color = AppPalette.GoldBright,
                letterSpacing = 0.14.em,
                fontSize = 10.sp,
            )
            HeaderActionButton(
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
                    shape = RoundedCornerShape(12.dp),
                    background = AppPalette.Gold.copy(alpha = 0.06f),
                    border = AppPalette.Gold.copy(alpha = 0.3f),
                ).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(suggestion.tag, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading)
            Text(
                suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        HeaderActionButton(
            text = stringResource(Res.string.session_detail_tag_add_suggestion_button),
            onClick = onAdd,
            foreground = AppPalette.Background,
            background = AppPalette.Gold,
            border = AppPalette.Gold,
        )
    }
}

@Composable
private fun TagCatalogSection(
    catalog: List<SessionTagCatalogItem>,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            text = stringResource(Res.string.session_detail_tag_catalog_header),
            color = AppPalette.TextTertiary,
            letterSpacing = 0.14.em,
            fontSize = 10.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            catalog.forEach { item ->
                TagCatalogChip(item, onClick = { eventSink(SessionDetailEvent.TagToggled(item.name)) })
            }
        }
    }
}

@Composable
private fun TagCatalogChip(
    item: SessionTagCatalogItem,
    onClick: () -> Unit,
) {
    val background = if (item.assigned) AppPalette.Gold else Color.Transparent
    val foreground = if (item.assigned) AppPalette.Background else AppPalette.TextDescription
    val border = if (item.assigned) AppPalette.Gold else AppPalette.Border
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .appCard(shape = RoundedCornerShape(20.dp), background = background, border = border, onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (item.assigned) "✓ ${item.name}" else item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = foreground,
        )
    }
}

@Composable
private fun TagCustomInputSection(
    draft: String,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            text = stringResource(Res.string.session_detail_tag_custom_header),
            color = AppPalette.TextTertiary,
            letterSpacing = 0.14.em,
            fontSize = 10.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val placeholder = stringResource(Res.string.session_detail_tag_draft_placeholder)
            val submit = { eventSink(SessionDetailEvent.TagDraftSubmitted) }
            BasicTextField(
                value = draft,
                onValueChange = { eventSink(SessionDetailEvent.TagDraftChanged(it)) },
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
                cursorBrush = GoldCursorBrush,
                keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .appCard(
                            shape = RoundedCornerShape(12.dp),
                            background = AppPalette.Background,
                            border = AppPalette.Border,
                        ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppPalette.TextTertiary,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            IconBadge(
                size = 48.dp,
                shape = RoundedCornerShape(12.dp),
                background = AppPalette.SurfaceInput,
                border = AppPalette.Border,
                onClick = submit,
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = AppPalette.GoldBright)
            }
        }
    }
}
