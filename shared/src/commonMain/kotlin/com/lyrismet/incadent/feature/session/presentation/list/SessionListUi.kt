package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppLoader
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.ListSectionHeader
import com.lyrismet.incadent.core.designsystem.component.NoPeek
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SwipeHintBanner
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.designsystem.component.rememberSwipeHintPlayback
import com.lyrismet.incadent.core.designsystem.component.scrollEdgeFade
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.feature.campaign.presentation.sheet.AppArchiveBanner
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheets
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSwitcherPill
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EntityCardSheet
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.quickedit.asSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityLoadingSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.entitySheetActions
import com.lyrismet.incadent.feature.session.presentation.list.downtime.DowntimeCardRow
import com.lyrismet.incadent.feature.session.presentation.list.downtime.DowntimeEvent
import com.lyrismet.incadent.feature.session.presentation.list.downtime.DowntimeSheets
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.campaign_switcher_description
import incadent.shared.generated.resources.session_list_archive_section
import incadent.shared.generated.resources.session_list_delete_content_description
import incadent.shared.generated.resources.session_list_edit_fixed
import incadent.shared.generated.resources.session_list_empty
import incadent.shared.generated.resources.session_list_empty_button
import incadent.shared.generated.resources.session_list_empty_title
import incadent.shared.generated.resources.session_list_new_session_button
import incadent.shared.generated.resources.session_list_title
import incadent.shared.generated.resources.swipe_hint_delete_only_title
import incadent.shared.generated.resources.swipe_hint_dismiss
import incadent.shared.generated.resources.swipe_hint_sessions_subtitle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val SETTLE_MILLIS = 800L
private const val MUTED_ALPHA = 0.55f
private const val HELD_ALPHA = 0.9f

@Composable
fun SessionListUi(
    state: SessionListState,
    modifier: Modifier = Modifier,
) {
    val swipeHint = rememberSwipeHintPlayback(state.swipeHintTarget, SwipeHintDirection.DELETE_ONLY)
    // the tab bar already sits above the navigation bar, so the floating hints must not add its inset again
    val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Scaffold(modifier = modifier, contentWindowInsets = contentWindowInsets) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionListBody(state) { swipeHint.peekOffsetPx }
            AppToast(
                text = state.toast?.text,
                modifier = floatingBarAnchor(),
            )
            SwipeHintBanner(
                // the campaign-rename toast shares this anchor, so the hint yields to it instead of stacking
                visible = swipeHint.bannerVisible && state.toast == null,
                direction = SwipeHintDirection.DELETE_ONLY,
                title = stringResource(Res.string.swipe_hint_delete_only_title),
                subtitle = stringResource(Res.string.swipe_hint_sessions_subtitle),
                dismissLabel = stringResource(Res.string.swipe_hint_dismiss),
                onDismiss = swipeHint::dismiss,
                modifier = floatingBarAnchor(),
            )
        }
    }

    DowntimeSheets(state.downtimeSheet) { state.eventSink(SessionListEvent.Downtime(it)) }
    CampaignSheets(state.campaignSheet) { event -> state.eventSink(SessionListEvent.CampaignSheet(event)) }

    if (state.entityLoading) {
        EntityLoadingSheet { state.eventSink(SessionListEvent.EntitySheet(EntitySheetEvent.Dismissed)) }
    }
    state.selectedEntity?.let { entity ->
        val onQuickEvent = { event: QuickEditUiEvent -> state.eventSink(SessionListEvent.QuickEdit(event)) }
        EntityCardSheet(
            item = entity,
            actions =
                entitySheetActions(
                    quickEdit = if (state.campaignArchived) null else state.quickEdit.asSheet(onQuickEvent),
                ) { state.eventSink(SessionListEvent.EntitySheet(it)) },
            quick = state.quickEdit,
            onQuickEvent = onQuickEvent,
            onDismiss = { state.eventSink(SessionListEvent.EntitySheet(EntitySheetEvent.Dismissed)) },
        )
    }
}

@Composable
private fun SessionListBody(
    state: SessionListState,
    swipeHintPeekOffsetPx: () -> Float,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            overline = state.campaignName,
            title = stringResource(Res.string.session_list_title),
            overlineContent = {
                CampaignSwitcherPill(
                    text = state.campaignName,
                    contentDescription = stringResource(Res.string.campaign_switcher_description),
                    onClick = { state.eventSink(SessionListEvent.CampaignSheet(CampaignSheetEvent.LibraryOpened)) },
                    muted = state.campaignArchived,
                )
            },
            titleTrailingContent =
                if (!state.campaignArchived && (state.liveSession != null || state.sessions.isNotEmpty())) {
                    {
                        EditModeButton(
                            state.editing,
                        ) { state.eventSink(SessionListEvent.Order(SessionOrderEvent.EditToggled)) }
                    }
                } else {
                    null
                },
            overlineTrailingContent = {
                if (!state.campaignArchived && !state.editing) {
                    AppButton(
                        text = stringResource(Res.string.session_list_new_session_button),
                        onClick = { state.eventSink(SessionListEvent.NewSessionClicked) },
                    )
                }
            },
        )
        val campaignId = state.campaignId
        if (state.campaignArchived && campaignId != null) {
            AppArchiveBanner(
                onResume = { state.eventSink(SessionListEvent.CampaignSheet(CampaignSheetEvent.Resumed(campaignId))) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoader() }
        } else if (state.liveSession == null && state.sessions.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.session_list_empty_title),
                text = stringResource(Res.string.session_list_empty),
                primaryAction =
                    if (state.campaignArchived) {
                        null
                    } else {
                        EmptyStateAction(label = stringResource(Res.string.session_list_empty_button)) {
                            state.eventSink(SessionListEvent.NewSessionClicked)
                        }
                    },
            )
        } else {
            SessionListContent(state, swipeHintPeekOffsetPx)
        }
    }
}

@Composable
private fun SessionListContent(
    state: SessionListState,
    swipeHintPeekOffsetPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val drag = remember { SessionDrag() }
    val reorderState = rememberReorderableLazyListState(listState) { from, to -> drag.move(from.key, to.key) }
    // the dates are saved in a moment, then what is listed is what the drag showed
    LaunchedEffect(drag.draggedId, state.sessions) {
        if (drag.draggedId == null) {
            delay(SETTLE_MILLIS)
            drag.settle()
        }
    }
    val downtimeBySession = remember(state.downtimes) { state.downtimes.associateBy { it.afterSessionId } }
    val downtimeItem: LazyListScope.(Long) -> Unit = { sessionId ->
        downtimeBySession[sessionId]?.let { card ->
            item(key = "downtime-${card.id}") {
                DowntimeCardRow(
                    card = card,
                    onClick = { state.eventSink(SessionListEvent.Downtime(DowntimeEvent.CardClicked(card.id))) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().scrollEdgeFade(listState),
        contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.editing) item(key = "edit-banner") { EditBanner() }
        state.liveSession?.let { live -> liveItems(state, live, downtimeItem) }
        item {
            ListSectionHeader(
                text = stringResource(Res.string.session_list_archive_section),
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                count = state.sessions.size,
            )
        }
        val rows = drag.shown(state.sessions)
        val dragging = drag.draggedId != null
        rows.forEachIndexed { index, session ->
            // a gap sits above the session before it, the list runs from the newest down
            if (!dragging) {
                if (downtimeBySession.containsKey(session.id)) {
                    downtimeItem(session.id)
                } else if (state.editing) {
                    val upper = state.positionLabels.getOrNull(rows.size - index)
                    if (upper != null) gapItem(state, session, upper, rows.size - index - 1)
                }
            }
            item(key = session.id) {
                SessionRow(state, session, index, rows.size, drag, reorderState, swipeHintPeekOffsetPx)
            }
        }
    }
}

private fun LazyListScope.liveItems(
    state: SessionListState,
    live: LiveSessionItem,
    downtimeItem: LazyListScope.(Long) -> Unit,
) {
    item {
        val onMentionClick = { ref: EntityRef ->
            state.eventSink(SessionListEvent.EntitySheet(EntitySheetEvent.EntityClicked(ref)))
        }
        if (state.editing) {
            LiveSessionCard(
                session = live,
                onClick = { state.eventSink(SessionListEvent.SessionClicked(live.id)) },
                onMentionClick = onMentionClick,
                modifier = Modifier.alpha(MUTED_ALPHA),
                label = state.positionLabels.lastOrNull() ?: live.numberLabel,
                footerNote = stringResource(Res.string.session_list_edit_fixed),
            )
        } else {
            SwipeToDeleteRow(
                onDeleteRequested = { state.eventSink(SessionListEvent.DeleteSessionClicked(live.id)) },
                deleteContentDescription = stringResource(Res.string.session_list_delete_content_description),
            ) {
                LiveSessionCard(
                    session = live,
                    onClick = { state.eventSink(SessionListEvent.SessionClicked(live.id)) },
                    onMentionClick = onMentionClick,
                )
            }
        }
    }
    if (state.editing) {
        item(key = "past-session") {
            PastSessionTile(onClick = { state.eventSink(SessionListEvent.Order(SessionOrderEvent.PastSessionAdded)) })
        }
    }
    downtimeItem(live.id)
}

private fun LazyListScope.gapItem(
    state: SessionListState,
    lower: SessionListItem,
    upperLabel: String,
    lowerPlace: Int,
) {
    item(key = "gap-${lower.id}") {
        val lowerLabel = state.positionLabels.getOrNull(lowerPlace) ?: lower.numberLabel
        GapTile(
            upperLabel = upperLabel,
            lowerLabel = lowerLabel,
            onClick = {
                state.eventSink(SessionListEvent.Downtime(DowntimeEvent.NewClicked(lower.id, lowerLabel, upperLabel)))
            },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun LazyItemScope.SessionRow(
    state: SessionListState,
    session: SessionListItem,
    index: Int,
    count: Int,
    drag: SessionDrag,
    reorderState: ReorderableLazyListState,
    swipeHintPeekOffsetPx: () -> Float,
) {
    if (!state.editing) {
        SessionArchiveRow(
            session = session,
            onClick = { state.eventSink(SessionListEvent.SessionClicked(session.id)) },
            onDeleteClick = { state.eventSink(SessionListEvent.DeleteSessionClicked(session.id)) },
            // only the row the swipe hint targets actually peeks - every other row stays at rest
            peekOffsetPx = if (session.id == state.swipeHintTarget?.itemId) swipeHintPeekOffsetPx else NoPeek,
            modifier = Modifier.animateItem(),
        )
        return
    }
    // the number is the place the session stands on, so it follows the row while it is carried
    val label = state.positionLabels.getOrNull(count - index - 1) ?: session.numberLabel
    ReorderableItem(reorderState, key = session.id) { held ->
        ArchiveCard(
            session = session,
            label = label,
            labelColor = if (label != session.numberLabel) AppPalette.GoldBright else AppPalette.GoldDim,
            onClick = { state.eventSink(SessionListEvent.SessionClicked(session.id)) },
            modifier = Modifier.alpha(if (held) HELD_ALPHA else 1f),
        ) {
            SessionHandle(
                Modifier.draggableHandle(
                    onDragStarted = { drag.start(session.id, state.sessions) },
                    onDragStopped = {
                        drag.end {
                            id,
                            at,
                            ->
                            state.eventSink(SessionListEvent.Order(SessionOrderEvent.SessionMoved(id, at)))
                        }
                    },
                ),
            )
        }
    }
}

@Composable
private fun SessionArchiveRow(
    session: SessionListItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    peekOffsetPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    SwipeToDeleteRow(
        onDeleteRequested = onDeleteClick,
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.session_list_delete_content_description),
        peekOffsetPx = peekOffsetPx,
    ) {
        ArchiveCard(session = session, label = session.numberLabel, labelColor = AppPalette.GoldDim, onClick = onClick)
    }
}
