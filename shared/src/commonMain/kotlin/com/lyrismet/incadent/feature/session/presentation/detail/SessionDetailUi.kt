package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormSheet
import com.lyrismet.incadent.feature.codex.presentation.form.FormDensity
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EntityCardSheet
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.quickedit.asSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.codex.presentation.summary.entitySheetActions
import com.lyrismet.incadent.feature.combat.presentation.summary.CombatFeedCard
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSnippetsSheet
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_results_delete_confirm
import dndplayerscodex.shared.generated.resources.combat_results_delete_keep
import dndplayerscodex.shared.generated.resources.combat_results_delete_text
import dndplayerscodex.shared.generated.resources.combat_results_delete_title
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionDetailUi(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    // AppTabHost's bottom tab bar already reserves the nav-bar inset, so this scaffold must not reserve it again
    val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Scaffold(modifier = modifier, contentWindowInsets = contentWindowInsets) { contentPadding ->
        // on the whole screen, not the list, which is only as tall as its notes
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding).clearSelectionOnTap(state.eventSink)) {
            SessionDetailBody(state)
        }
    }

    state.selectedEntity?.let { entity -> SessionDetailEntitySheet(state, entity) }
    if (state.combatDeleteVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.combat_results_delete_title),
            text = stringResource(Res.string.combat_results_delete_text),
            onConfirm = { state.eventSink(SessionDetailEvent.CombatDeleteConfirmed) },
            onDismiss = { state.eventSink(SessionDetailEvent.CombatDeleteDismissed) },
            confirmLabel = stringResource(Res.string.combat_results_delete_confirm),
            dismissLabel = stringResource(Res.string.combat_results_delete_keep),
        )
    }
    state.tagSheet?.let { sheet -> SessionDetailTagsSheet(sheet, state.eventSink) }
    state.deletedMentionSheet?.let { sheet -> DeletedMentionSheet(sheet, state.eventSink) }
    state.mentionCreateSheet?.let { sheet -> MentionCreateSheet(sheet, state.eventSink) }
    state.entryForm?.let { form ->
        CodexEntryFormSheet(
            form,
            state.toast,
            density = FormDensity.Compact,
        ) { event -> state.eventSink(SessionDetailEvent.EntryForm(event)) }
    }
    state.phraseSheet?.let { sheet ->
        PhraseSnippetsSheet(sheet) { event -> state.eventSink(SessionDetailEvent.PhraseSheet(event)) }
    }
}

@Composable
private fun ColumnScope.SessionDetailBody(state: SessionDetailState) {
    SessionDetailHeader(state.header, state.eventSink)

    // the toast floats at the feed's bottom edge, so it never covers the composer
    Box(modifier = Modifier.weight(1f)) {
        if (state.feed.items.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.session_detail_empty_feed),
                text = stringResource(Res.string.session_detail_empty_feed_hint),
                ornament = true,
            )
        } else {
            SessionFeed(state.feed, state.eventSink)
        }
        AppToast(
            text = state.toast,
            modifier = floatingBarAnchor(),
        )
    }

    // an ended session only shows the input while one of its notes is being edited
    if ((state.header.isLive || state.isEditingEntry) && !state.readOnly) {
        SessionComposer(state)
    }
}

@Composable
private fun SessionFeed(
    state: SessionFeedState,
    eventSink: (SessionDetailEvent) -> Unit,
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
        items(state.items, key = { it.key }) { feedItem ->
            when (feedItem) {
                is SessionFeedItem.DaySeparator -> {
                    DaySeparatorRow(feedItem.label)
                }

                is SessionFeedItem.Combat -> {
                    CombatFeedCard(
                        item = feedItem.card,
                        onDelete =
                            { eventSink(SessionDetailEvent.CombatDeleteClicked(feedItem.card.id)) }
                                .takeUnless { state.readOnly },
                        modifier = Modifier.animateItem(),
                    )
                }

                is SessionFeedItem.Note -> NoteRow(state, eventSink, feedItem.entry, Modifier.animateItem())
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
    LaunchedEffect(state.items.size, state.highlightedEntryId) {
        if (state.items.isNotEmpty() && state.highlightedEntryId == null && !focusResolved) {
            listState.animateScrollToItem(state.items.lastIndex)
        }
    }
}

@Composable
private fun NoteRow(
    state: SessionFeedState,
    send: (SessionDetailEvent) -> Unit,
    entry: SessionEntryItem,
    modifier: Modifier,
) {
    SessionEntryRow(
        entry = entry,
        isSelected = state.selectedEntryId == entry.id,
        isEditing = state.editingEntryId == entry.id,
        isHighlighted = state.highlightedEntryId == entry.id,
        onClick = { send(SessionDetailEvent.EntryClicked(entry.id)) },
        onEditClick = { send(SessionDetailEvent.EditEntryClicked(entry.id)) },
        onSavePhraseClick = { send(SessionDetailEvent.SaveEntryAsPhraseClicked(entry.id)) },
        onDeleteClick = { send(SessionDetailEvent.DeleteEntryClicked(entry.id)) },
        onMentionClick = { ref -> send(SessionDetailEvent.EntitySheet(EntitySheetEvent.EntityClicked(ref))) },
        mentionActions = entryMentionActions(entry.id, send),
        modifier = modifier,
    )
}

@Composable
private fun SessionDetailEntitySheet(
    state: SessionDetailState,
    entity: EntitySummaryItem,
) {
    val onQuickEvent = { event: QuickEditUiEvent -> state.eventSink(SessionDetailEvent.QuickEdit(event)) }
    EntityCardSheet(
        item = entity,
        actions =
            entitySheetActions(
                speciesNoteEdit = !state.readOnly,
                quickEdit = if (state.readOnly) null else state.quickEdit.asSheet(onQuickEvent),
            ) { state.eventSink(SessionDetailEvent.EntitySheet(it)) },
        quick = state.quickEdit,
        onQuickEvent = onQuickEvent,
        onDismiss = { state.eventSink(SessionDetailEvent.EntitySheet(EntitySheetEvent.Dismissed)) },
    )
}

private const val FOCUS_SCROLL_OFFSET_DP = 72

private fun entryMentionActions(
    entryId: Long,
    eventSink: (SessionDetailEvent) -> Unit,
) = EntryMentionActions(
    onDeletedClick = { eventSink(SessionDetailEvent.DeletedMentionClicked(it)) },
    onAutoLink = { eventSink(SessionDetailEvent.AutoMentionsLinkClicked(entryId)) },
    onAutoDismiss = { eventSink(SessionDetailEvent.AutoMentionsDismissed(entryId)) },
    onAutoWordClick = { key -> eventSink(SessionDetailEvent.AutoMentionWordClicked(entryId, key)) },
    onSpeciesClick = { occurrence, id -> eventSink(SessionDetailEvent.SpeciesChipClicked(entryId, occurrence, id)) },
)

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

// a tap no note, field or button consumed lands on free space and drops the note selection and the title editor
private fun Modifier.clearSelectionOnTap(eventSink: (SessionDetailEvent) -> Unit): Modifier =
    composed {
        val currentSink by rememberUpdatedState(eventSink)
        pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                // waitForUpOrCancellation(Final) would swallow the release itself, so the Final pass is read by hand
                if (awaitTapReleaseUnconsumed(down.position, viewConfiguration.touchSlop)) {
                    currentSink(SessionDetailEvent.OutsideTapped)
                }
            }
        }
    }

private suspend fun AwaitPointerEventScope.awaitTapReleaseUnconsumed(
    start: Offset,
    slop: Float,
): Boolean {
    var result: Boolean? = null
    while (result == null) {
        val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull()
        val moved = change != null && (change.position - start).getDistance() > slop
        result =
            when {
                change == null || change.isConsumed || moved -> false
                !change.pressed -> true
                else -> null
            }
    }
    return result
}
