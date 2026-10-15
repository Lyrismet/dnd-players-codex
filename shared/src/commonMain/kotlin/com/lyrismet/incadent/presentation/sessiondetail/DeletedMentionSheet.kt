package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.domain.model.EntityKind
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.deleted_mention_back
import dndplayerscodex.shared.generated.resources.deleted_mention_body
import dndplayerscodex.shared.generated.resources.deleted_mention_create_format
import dndplayerscodex.shared.generated.resources.deleted_mention_link_hint
import dndplayerscodex.shared.generated.resources.deleted_mention_link_title
import dndplayerscodex.shared.generated.resources.deleted_mention_overline
import dndplayerscodex.shared.generated.resources.deleted_mention_search_placeholder
import dndplayerscodex.shared.generated.resources.deleted_mention_text_hint
import dndplayerscodex.shared.generated.resources.deleted_mention_text_title
import dndplayerscodex.shared.generated.resources.deleted_mention_type_location
import dndplayerscodex.shared.generated.resources.deleted_mention_type_npc
import dndplayerscodex.shared.generated.resources.deleted_mention_type_party
import dndplayerscodex.shared.generated.resources.deleted_mention_type_quest
import dndplayerscodex.shared.generated.resources.mention_count_in_sessions
import dndplayerscodex.shared.generated.resources.mention_count_notes
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_party
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** what to do with a "?" tag - make the record again, link its mentions to another one, or keep the name as words */
@Composable
internal fun DeletedMentionSheet(
    sheet: DeletedMentionSheetState,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { eventSink(SessionDetailEvent.DeletedMentionSheetDismissed) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DeletedMentionHeader(sheet.name) { eventSink(SessionDetailEvent.DeletedMentionSheetDismissed) }
            if (sheet.isPicking) {
                DeletedMentionPicker(sheet, eventSink)
            } else {
                DeletedMentionActions(sheet, eventSink)
            }
        }
    }
}

// the same overline and "? name" over both steps, so picking a record does not retitle the sheet
@Composable
private fun DeletedMentionHeader(
    name: String,
    onClose: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            SectionOverline(
                text = stringResource(Res.string.deleted_mention_overline),
                color = AppPalette.TextSecondary,
                letterSpacing = 0.16.em,
            )
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = AppPalette.GoldBright)) { append("? ") }
                        append(name)
                    },
                style = MaterialTheme.typography.headlineLarge,
                color = AppPalette.TextHeading,
            )
        }
        SheetCloseButton(onClick = onClose)
    }
}

@Composable
private fun DeletedMentionActions(
    sheet: DeletedMentionSheetState,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    val notes = pluralStringResource(Res.plurals.mention_count_notes, sheet.notes, sheet.notes)
    val sessions = pluralStringResource(Res.plurals.mention_count_in_sessions, sheet.sessions, sheet.sessions)
    Text(
        stringResource(Res.string.deleted_mention_body, notes, sessions),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
        color = AppPalette.TextMuted,
    )
    FormPrimaryButton(
        text =
            stringResource(
                Res.string.deleted_mention_create_format,
                sheet.name,
                stringResource(sheet.type.typeLabelRes()),
            ),
        onClick = { eventSink(SessionDetailEvent.DeletedMentionCreateClicked) },
    )
    DeletedMentionOption(
        title = stringResource(Res.string.deleted_mention_link_title),
        hint = stringResource(Res.string.deleted_mention_link_hint),
        onClick = { eventSink(SessionDetailEvent.DeletedMentionPickingChanged(true)) },
    )
    DeletedMentionOption(
        title = stringResource(Res.string.deleted_mention_text_title),
        hint = stringResource(Res.string.deleted_mention_text_hint),
        onClick = { eventSink(SessionDetailEvent.DeletedMentionKeptAsText) },
    )
}

@Composable
private fun DeletedMentionOption(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(14.dp),
                    background = Color.Transparent,
                    border = AppPalette.BorderHover,
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), color = AppPalette.TextPrimary)
        Text(hint, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = AppPalette.TextSecondary)
    }
}

// the codex records to link to - search over a card of rows, each in its record's colour with its kind on the right
@Composable
private fun DeletedMentionPicker(
    sheet: DeletedMentionSheetState,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    AppSearchField(
        query = sheet.query,
        onQueryChange = { eventSink(SessionDetailEvent.DeletedMentionQueryChanged(it)) },
        placeholder = stringResource(Res.string.deleted_mention_search_placeholder),
        height = 44.dp,
        textSize = 14.sp,
        background = AppPalette.Background,
    )
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = PICKER_MAX_HEIGHT)
                .appCard(shape = RoundedCornerShape(14.dp), background = AppPalette.Background),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
    ) {
        itemsIndexed(sheet.linkOptions, key = { _, option -> option.id }) { index, option ->
            if (index > 0) AppDivider(color = AppPalette.BorderSubtle)
            DeletedMentionLinkRow(option) { eventSink(SessionDetailEvent.DeletedMentionLinked(option.id)) }
        }
    }
    HeaderActionButton(
        text = stringResource(Res.string.deleted_mention_back),
        onClick = { eventSink(SessionDetailEvent.DeletedMentionPickingChanged(false)) },
        modifier = Modifier.heightIn(min = 40.dp),
        foreground = AppPalette.TextDescription,
        background = Color.Transparent,
        border = AppPalette.BorderHover,
    )
}

private val PICKER_MAX_HEIGHT = 340.dp

@Composable
private fun DeletedMentionLinkRow(
    option: DeletedMentionLinkOption,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            option.glyph.symbol,
            fontSize = 9.sp,
            color = option.color.foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(14.dp),
        )
        Text(
            option.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = option.color.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(option.type.kindLabelRes()),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = AppPalette.TextSecondary,
        )
    }
}

private fun EntityKind.kindLabelRes(): StringResource =
    when (this) {
        EntityKind.NPC -> Res.string.session_detail_mention_type_npc
        EntityKind.PARTY -> Res.string.session_detail_mention_type_party
        EntityKind.QUEST -> Res.string.session_detail_mention_type_quest
        EntityKind.LOCATION -> Res.string.session_detail_mention_type_location
    }

private fun EntityKind.typeLabelRes(): StringResource =
    when (this) {
        EntityKind.NPC -> Res.string.deleted_mention_type_npc
        EntityKind.PARTY -> Res.string.deleted_mention_type_party
        EntityKind.QUEST -> Res.string.deleted_mention_type_quest
        EntityKind.LOCATION -> Res.string.deleted_mention_type_location
    }
