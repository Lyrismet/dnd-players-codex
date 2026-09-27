package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.ConfirmationDialog
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.MentionChip
import com.lyrismet.dndcodex.core.designsystem.component.MentionChipItem
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.session_detail_back
import dndplayerscodex.shared.generated.resources.session_detail_delete_entry_title
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed
import dndplayerscodex.shared.generated.resources.session_detail_end_button
import dndplayerscodex.shared.generated.resources.session_detail_ended_badge
import dndplayerscodex.shared.generated.resources.session_detail_live_badge
import dndplayerscodex.shared.generated.resources.session_detail_resume_button
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionDetailUi(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    // pending confirmation before an entry is actually deleted - purely a transient ui flag
    var pendingDeleteEntry by remember { mutableStateOf<SessionEntryItem?>(null) }

    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionDetailHeader(state)

            if (state.feed.isEmpty()) {
                EmptyStatePlaceholder(
                    text = stringResource(Res.string.session_detail_empty_feed),
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.feed, key = { it.key }) { feedItem ->
                        when (feedItem) {
                            is SessionFeedItem.DaySeparator -> DaySeparatorRow(feedItem.label)
                            is SessionFeedItem.Note ->
                                SessionEntryRow(
                                    entry = feedItem.entry,
                                    onDeleteClick = { pendingDeleteEntry = feedItem.entry },
                                    onMentionClick = { npcId ->
                                        state.eventSink(SessionDetailEvent.MentionChipClicked(npcId))
                                    },
                                )
                        }
                    }
                }
            }

            if (state.isLive) {
                SessionComposer(state)
            }
        }
    }

    pendingDeleteEntry?.let { entry ->
        ConfirmationDialog(
            title = stringResource(Res.string.session_detail_delete_entry_title),
            text = entry.segments.joinToString("") { segment -> segment.plainText() },
            onConfirm = {
                state.eventSink(SessionDetailEvent.DeleteEntryClicked(entry.id))
                pendingDeleteEntry = null
            },
            onDismiss = { pendingDeleteEntry = null },
        )
    }
}

private fun SessionEntrySegment.plainText(): String =
    when (this) {
        is SessionEntrySegment.Text -> text
        is SessionEntrySegment.Mention -> chip.label
    }

@Composable
private fun SessionDetailHeader(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { state.eventSink(SessionDetailEvent.BackClicked) }) {
                Text(
                    stringResource(Res.string.session_detail_back),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.GoldBright,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SessionStatusBadge(isLive = state.isLive)
                if (state.isLive) {
                    HeaderActionButton(
                        text = stringResource(Res.string.session_detail_end_button),
                        onClick = { state.eventSink(SessionDetailEvent.EndSessionClicked) },
                        foreground = AppPalette.MaroonBright,
                        background = AppPalette.Maroon.copy(alpha = 0.16f),
                        border = AppPalette.Maroon.copy(alpha = 0.7f),
                    )
                } else {
                    HeaderActionButton(
                        text = stringResource(Res.string.session_detail_resume_button),
                        onClick = { state.eventSink(SessionDetailEvent.ResumeSessionClicked) },
                    )
                }
            }
        }
        Text(
            state.overline,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.Gold,
            modifier = Modifier.padding(top = 4.dp),
        )
        // BasicTextField, not Material3's TextField - its built-in content padding and minimum height
        // are exactly the "extra padding around the title" the design doesn't have (padding:0)
        BasicTextField(
            value = state.title,
            onValueChange = { state.eventSink(SessionDetailEvent.TitleChanged(it)) },
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = AppPalette.TextHeading),
            singleLine = true,
            cursorBrush = SolidColor(AppPalette.Gold),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
        Text(state.dateLabel, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
        if (state.headerMentions.isNotEmpty()) {
            HeaderMentionsRow(state.headerMentions, state.eventSink, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun HeaderMentionsRow(
    mentions: List<MentionChipItem>,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mentions.forEach { chip ->
            MentionChip(
                item = chip,
                onClick = chip.npcId?.let { npcId -> { eventSink(SessionDetailEvent.MentionChipClicked(npcId)) } },
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SessionStatusBadge(
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isLive) {
        Row(
            modifier =
                modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppPalette.Emerald.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(AppPalette.EmeraldBright))
            Text(
                stringResource(Res.string.session_detail_live_badge),
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.EmeraldBright,
            )
        }
    } else {
        Text(
            stringResource(Res.string.session_detail_ended_badge),
            style = MaterialTheme.typography.labelMedium,
            color = AppPalette.TextTertiary,
            modifier = modifier,
        )
    }
}

@Composable
private fun DaySeparatorRow(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.Parchment)
        Box(
            modifier =
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
                    .height(1.dp)
                    .background(AppPalette.Border),
        )
    }
}

// long-press to reveal actions, Telegram-style, instead of a permanently visible delete icon
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionEntryRow(
    entry: SessionEntryItem,
    onDeleteClick: () -> Unit,
    onMentionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                entry.timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
                modifier = Modifier.width(38.dp),
            )
            SessionEntryBody(entry.segments, onMentionClick, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_delete), color = AppPalette.MaroonBright) },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = AppPalette.MaroonBright)
                },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
            )
        }
    }
}

// wraps word-by-word like the note text around it, so a chip never gets stranded on its own line
@Composable
private fun SessionEntryBody(
    segments: List<SessionEntrySegment>,
    onMentionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is SessionEntrySegment.Text ->
                    segment.text.split(' ').forEach { word ->
                        if (word.isNotEmpty()) {
                            Text(word, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
                        }
                    }

                is SessionEntrySegment.Mention ->
                    MentionChip(
                        item = segment.chip,
                        onClick = segment.chip.npcId?.let { npcId -> { onMentionClick(npcId) } },
                        fontSize = 14.sp,
                    )
            }
        }
    }
}
