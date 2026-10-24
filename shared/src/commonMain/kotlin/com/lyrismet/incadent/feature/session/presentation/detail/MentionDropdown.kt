package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.format.joinWithDot
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.mention_create_row_format
import dndplayerscodex.shared.generated.resources.mention_empty_codex
import dndplayerscodex.shared.generated.resources.mention_nickname_format
import org.jetbrains.compose.resources.stringResource

/** the rows of the "@" dropdown, laid out by the composer's popover chrome */
@Composable
internal fun MentionDropdownContent(
    items: List<MentionDropdownItem>,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    items.forEach { item ->
        when (item) {
            is MentionDropdownItem.Header -> MentionDropdownHeader(item.title)
            is MentionDropdownItem.Entry -> MentionDropdownEntry(item.suggestion, eventSink)
            is MentionDropdownItem.Create -> MentionDropdownCreate(item.name, eventSink)
            MentionDropdownItem.EmptyCodex ->
                Text(
                    stringResource(Res.string.mention_empty_codex),
                    style = AppText.caption,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
        }
    }
}

@Composable
private fun MentionDropdownHeader(title: String) {
    Text(
        title,
        style = AppText.fieldOverline,
        color = AppPalette.TextTertiary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun MentionDropdownEntry(
    suggestion: SessionMentionSuggestion,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.Tile)
                .clickable { eventSink(SessionDetailEvent.MentionSuggestionPicked(suggestion.token)) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(14.dp), contentAlignment = Alignment.Center) {
            Text(suggestion.glyph.symbol, fontSize = AppFontSize.Micro, color = suggestion.tint)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                suggestion.name,
                fontSize = AppFontSize.Medium,
                lineHeight = 20.sp,
                color = AppPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val alias =
                suggestion.nickname
                    .takeIf {
                        it.isNotEmpty()
                    }?.let { stringResource(Res.string.mention_nickname_format, it) }
            val detail = joinWithDot(alias.orEmpty(), suggestion.detail)
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    fontSize = AppFontSize.Caption,
                    lineHeight = 16.sp,
                    color = AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            suggestion.typeLabel,
            fontSize = AppFontSize.Caption,
            lineHeight = 16.sp,
            color = AppPalette.TextSecondary,
        )
    }
}

@Composable
private fun MentionDropdownCreate(
    name: String,
    eventSink: (SessionDetailEvent) -> Unit,
) {
    Text(
        stringResource(Res.string.mention_create_row_format, name),
        fontSize = AppFontSize.Medium,
        lineHeight = 20.sp,
        color = AppPalette.GoldBright,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.Tile)
                .clickable { eventSink(SessionDetailEvent.MentionCreateOpened(name)) }
                .padding(horizontal = 8.dp, vertical = 11.dp),
    )
}
