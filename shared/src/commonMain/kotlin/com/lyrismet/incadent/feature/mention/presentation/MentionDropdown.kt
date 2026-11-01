package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import dndplayerscodex.shared.generated.resources.mention_create_compact
import dndplayerscodex.shared.generated.resources.mention_create_hint
import dndplayerscodex.shared.generated.resources.mention_create_row_format
import dndplayerscodex.shared.generated.resources.mention_empty_codex
import dndplayerscodex.shared.generated.resources.mention_nickname_format
import org.jetbrains.compose.resources.stringResource

/** the rows of the "@" dropdown, laid out by the composer's popover chrome */
@Composable
internal fun MentionDropdownContent(
    items: List<MentionDropdownItem>,
    onPick: (token: String) -> Unit,
    onCreate: (name: String) -> Unit,
) {
    items.forEach { item ->
        when (item) {
            is MentionDropdownItem.Header -> MentionDropdownHeader(item.title)
            is MentionDropdownItem.Entry -> MentionDropdownEntry(item.suggestion, onPick)
            is MentionDropdownItem.Create -> MentionDropdownCreate(item.name, onCreate)
            is MentionDropdownItem.CreateRecord -> MentionDropdownCreateRecord(item.withHint) { onCreate("") }
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
    suggestion: MentionSuggestion,
    onPick: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.Tile)
                .clickable { onPick(suggestion.token) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(14.dp), contentAlignment = Alignment.Center) {
            MentionGlyphMark(suggestion.glyph, suggestion.tint, size = 14.dp)
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
    onCreate: (String) -> Unit,
) {
    CreateRow(onClick = { onCreate(name) }) {
        Text(
            stringResource(Res.string.mention_create_row_format, name),
            fontSize = AppFontSize.Medium,
            lineHeight = 20.sp,
            color = AppPalette.GoldBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CreateRow(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.Tile)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 11.dp),
        content = content,
    )
}

@Composable
private fun MentionDropdownCreateRecord(
    withHint: Boolean,
    onClick: () -> Unit,
) {
    CreateRow(onClick = onClick) {
        Text(
            stringResource(Res.string.mention_create_compact),
            fontSize = AppFontSize.Medium,
            lineHeight = 20.sp,
            color = AppPalette.GoldBright,
        )
        if (withHint) {
            Text(
                stringResource(Res.string.mention_create_hint),
                fontSize = AppFontSize.Caption,
                lineHeight = 16.sp,
                color = AppPalette.TextSecondary,
            )
        }
    }
}
