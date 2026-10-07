package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.incadent.core.designsystem.component.OrnamentedEmptyState
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionDetailUi(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionDetailHeader(state)

            if (state.feed.isEmpty()) {
                OrnamentedEmptyState(
                    title = stringResource(Res.string.session_detail_empty_feed),
                    hint = stringResource(Res.string.session_detail_empty_feed_hint),
                    modifier = Modifier.weight(1f),
                )
            } else {
                SessionFeed(state, modifier = Modifier.weight(1f))
            }

            if (state.isLive) {
                SessionComposer(state)
            }
        }
    }

    state.selectedEntity?.let { entity -> SessionDetailEntitySheet(state, entity) }
}

@Composable
private fun SessionFeed(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusScrollOffsetPx = with(LocalDensity.current) { -FOCUS_SCROLL_OFFSET_DP.dp.roundToPx() }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.feed, key = { it.key }) { feedItem ->
            when (feedItem) {
                is SessionFeedItem.DaySeparator -> {
                    DaySeparatorRow(feedItem.label)
                }

                is SessionFeedItem.Note -> {
                    SessionEntryRow(
                        entry = feedItem.entry,
                        isSelected = state.selectedEntryId == feedItem.entry.id,
                        isEditing = state.editingEntryId == feedItem.entry.id,
                        isHighlighted = state.highlightedEntryId == feedItem.entry.id,
                        onClick = {
                            state.eventSink(SessionDetailEvent.EntryClicked(feedItem.entry.id))
                        },
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
    }
    // tracks whether this highlight request ever found its target, reset whenever a new one starts
    var focusResolved by remember { mutableStateOf(false) }
    LaunchedEffect(state.highlightedEntryId) {
        if (state.highlightedEntryId != null) focusResolved = false
    }
    // scrolls to the presenter's focus index, which is null until the target entry is in the feed
    LaunchedEffect(state.focusIndex) {
        state.focusIndex?.let { index ->
            focusResolved = true
            listState.animateScrollToItem(index, focusScrollOffsetPx)
        }
    }
    // always jump to the newest entry, chat-app style - no "stay where the user scrolled" tracking
    // also falls back here once a highlight clears without ever resolving (e.g. its entry was deleted),
    // instead of leaving the list stuck wherever it happened to be when the highlight was requested
    LaunchedEffect(state.feed.size, state.highlightedEntryId) {
        if (state.feed.isNotEmpty() && state.highlightedEntryId == null && !focusResolved) {
            listState.animateScrollToItem(state.feed.lastIndex)
        }
    }
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
                    onNpcLifeSelected = { id, life ->
                        state.eventSink(SessionDetailEvent.NpcLifeSelected(id, life))
                    },
                    onQuestStatusSelected = { id, status ->
                        state.eventSink(SessionDetailEvent.QuestStatusSelected(id, status))
                    },
                    onRelatedNoteClicked = { id, entryId ->
                        state.eventSink(SessionDetailEvent.RelatedNoteClicked(id, entryId))
                    },
                ),
            onClose = { state.eventSink(SessionDetailEvent.SheetDismissed) },
        )
    }
}

private const val FOCUS_SCROLL_OFFSET_DP = 72

@Composable
private fun DaySeparatorRow(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
            color = AppPalette.Parchment,
        )
        AppDivider(modifier = Modifier.padding(start = 10.dp).weight(1f), color = AppPalette.Border)
    }
}
