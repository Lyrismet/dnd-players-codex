package com.lyrismet.dndcodex.presentation.sessiondetail

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.AppBottomSheet
import com.lyrismet.dndcodex.core.designsystem.component.AppDivider
import com.lyrismet.dndcodex.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.dndcodex.core.designsystem.component.OrnamentedEmptyState
import com.lyrismet.dndcodex.core.entitysummary.EntitySummaryItem
import com.lyrismet.dndcodex.core.entitysummary.EntitySummarySheetActions
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
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
