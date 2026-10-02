package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.GoldCursorBrush
import com.lyrismet.dndcodex.core.designsystem.component.AppBottomSheet
import com.lyrismet.dndcodex.core.designsystem.component.AppDivider
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.dndcodex.core.designsystem.component.GlowingDot
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.MentionChip
import com.lyrismet.dndcodex.core.designsystem.component.MentionChipItem
import com.lyrismet.dndcodex.core.designsystem.component.appCard
import com.lyrismet.dndcodex.core.designsystem.component.icons.AppIcons
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import com.lyrismet.dndcodex.core.entitysummary.EntitySummaryItem
import com.lyrismet.dndcodex.core.entitysummary.EntitySummarySheetActions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.action_edit
import dndplayerscodex.shared.generated.resources.session_detail_back
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed
import dndplayerscodex.shared.generated.resources.session_detail_end_button
import dndplayerscodex.shared.generated.resources.session_detail_ended_badge
import dndplayerscodex.shared.generated.resources.session_detail_live_badge
import dndplayerscodex.shared.generated.resources.session_detail_resume_button
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val ENTRY_COLLAPSE_ANIMATION_DURATION_MS = 220
private const val ENTRY_REMOVAL_GRACE_MS = 1000L

// top and bottom edges converge on the center as the row collapses, Gmail-delete-style, not a one-sided slide
private fun entryCollapseExit() =
    shrinkVertically(
        animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS),
        shrinkTowards = Alignment.CenterVertically,
    ) + fadeOut(animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS))

@Composable
fun SessionDetailUi(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionDetailHeader(state)

            if (state.feed.isEmpty()) {
                EmptyStatePlaceholder(
                    text = stringResource(Res.string.session_detail_empty_feed),
                    modifier = Modifier.weight(1f),
                )
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
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
                                    onEditClick = {
                                        state.eventSink(SessionDetailEvent.EditEntryClicked(feedItem.entry.id))
                                    },
                                    onDeleteClick = {
                                        state.eventSink(SessionDetailEvent.DeleteEntryClicked(feedItem.entry.id))
                                    },
                                    onMentionClick = { ref ->
                                        state.eventSink(SessionDetailEvent.MentionChipClicked(ref))
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                        }
                    }
                }
                // always jump to the newest entry, chat-app style - no "stay where the user scrolled" tracking
                LaunchedEffect(state.feed.size) {
                    if (state.feed.isNotEmpty()) {
                        listState.animateScrollToItem(state.feed.lastIndex)
                    }
                }
            }

            if (state.isLive) {
                SessionComposer(state)
            }
        }
    }

    state.selectedEntity?.let { entity -> SessionDetailEntitySheet(state, entity) }
}

@Composable
private fun SessionDetailEntitySheet(
    state: SessionDetailState,
    entity: EntitySummaryItem,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(SessionDetailEvent.SheetDismissed) }) {
        EntitySummarySheetContent(
            item = entity,
            actions =
                EntitySummarySheetActions(
                    onEntityRefClicked = { ref -> state.eventSink(SessionDetailEvent.MentionChipClicked(ref)) },
                    onNpcStatusSelected = { id, status ->
                        state.eventSink(SessionDetailEvent.NpcStatusSelected(id, status))
                    },
                    onQuestStatusSelected = { id, status ->
                        state.eventSink(SessionDetailEvent.QuestStatusSelected(id, status))
                    },
                    onRelatedNoteClicked = { id -> state.eventSink(SessionDetailEvent.RelatedNoteClicked(id)) },
                ),
            onClose = { state.eventSink(SessionDetailEvent.SheetDismissed) },
        )
    }
}

@Composable
private fun SessionDetailHeader(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SessionDetailHeaderContent(state)
        AppDivider()
    }
}

@Composable
private fun SessionDetailHeaderContent(
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
        // BasicTextField, not Material3's TextField, whose built-in padding and minimum height don't match the design
        BasicTextField(
            value = state.title,
            onValueChange = { state.eventSink(SessionDetailEvent.TitleChanged(it)) },
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = AppPalette.TextHeading),
            singleLine = true,
            cursorBrush = GoldCursorBrush,
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
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mentions.forEach { chip ->
            MentionChip(
                item = chip,
                onClick = chip.entityRef?.let { ref -> { eventSink(SessionDetailEvent.MentionChipClicked(ref)) } },
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
                    .appCard(
                        shape = RoundedCornerShape(7.dp),
                        background = AppPalette.Emerald.copy(alpha = 0.14f),
                        border = null,
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowingDot(dotSize = 6.dp)
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
        AppDivider(modifier = Modifier.padding(start = 10.dp).weight(1f), color = AppPalette.Border)
    }
}

// long-press to reveal actions, Telegram-style, instead of a permanently visible delete icon
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionEntryRow(
    entry: SessionEntryItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMentionClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    // collapses in place first, Gmail-style, instead of just vanishing once the entry is actually deleted
    var isRemoving by remember { mutableStateOf(false) }
    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(ENTRY_COLLAPSE_ANIMATION_DURATION_MS.toLong())
            onDeleteClick()
            // still composed after the grace period means nothing was deleted, so bring the entry back
            delay(ENTRY_REMOVAL_GRACE_MS)
            isRemoving = false
        }
    }

    AnimatedVisibility(
        visible = !isRemoving,
        enter = EnterTransition.None,
        exit = entryCollapseExit(),
        modifier = modifier,
    ) {
        Box {
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
                    text = { Text(stringResource(Res.string.action_edit), color = AppPalette.GoldBright) },
                    leadingIcon = {
                        Icon(AppIcons.Edit, contentDescription = null, tint = AppPalette.GoldBright)
                    },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.action_delete), color = AppPalette.MaroonBright) },
                    leadingIcon = {
                        Icon(AppIcons.Delete, contentDescription = null, tint = AppPalette.MaroonBright)
                    },
                    onClick = {
                        showMenu = false
                        isRemoving = true
                    },
                )
            }
        }
    }
}

// wraps word-by-word like the note text around it, so a chip never gets stranded on its own line
@Composable
private fun SessionEntryBody(
    segments: List<SessionEntrySegment>,
    onMentionClick: (EntityRef) -> Unit,
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
                        onClick = segment.chip.entityRef?.let { ref -> { onMentionClick(ref) } },
                        fontSize = 14.sp,
                    )
            }
        }
    }
}
