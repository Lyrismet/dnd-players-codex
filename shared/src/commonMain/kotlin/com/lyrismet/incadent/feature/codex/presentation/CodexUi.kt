package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.SwipeHintBanner
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.designsystem.component.rememberSwipeHintPlayback
import com.lyrismet.incadent.core.preferences.EntityEditMode
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormSheet
import com.lyrismet.incadent.feature.codex.presentation.form.PlaceSelectSheet
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummarySheetActions
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummarySheetContent
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.entitySheetActions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_add_entry_button
import dndplayerscodex.shared.generated.resources.codex_delete_warning_cancel
import dndplayerscodex.shared.generated.resources.codex_delete_warning_confirm
import dndplayerscodex.shared.generated.resources.codex_delete_warning_text
import dndplayerscodex.shared.generated.resources.codex_delete_warning_title
import dndplayerscodex.shared.generated.resources.codex_filter_all
import dndplayerscodex.shared.generated.resources.codex_overline
import dndplayerscodex.shared.generated.resources.codex_search_placeholder
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import dndplayerscodex.shared.generated.resources.codex_title
import dndplayerscodex.shared.generated.resources.mention_count_in_sessions
import dndplayerscodex.shared.generated.resources.mention_count_mentions
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_both_subtitle
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_both_title
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_quick_subtitle
import dndplayerscodex.shared.generated.resources.swipe_hint_delete_only_title
import dndplayerscodex.shared.generated.resources.swipe_hint_dismiss
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CodexUi(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    val tabItems = codexTabItems(state)
    val swipeHint = rememberSwipeHintPlayback(state.swipeHintTarget, state.swipeHintDirection)

    Scaffold(modifier = modifier) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    overline = stringResource(Res.string.codex_overline),
                    title = stringResource(Res.string.codex_title),
                    overlineTrailingContent = {
                        if (!state.readOnly) {
                            AppButton(
                                text = stringResource(Res.string.codex_add_entry_button),
                                onClick = { state.eventSink(CodexEvent.AddEntryClicked) },
                            )
                        }
                    },
                )
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    AppSearchField(
                        query = state.searchQuery,
                        onQueryChange = { state.eventSink(CodexEvent.SearchQueryChanged(it)) },
                        placeholder = stringResource(Res.string.codex_search_placeholder),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    AppSegmentedTabs(
                        tabs = tabItems,
                        selected = state.activeTab,
                        onSelected = { state.eventSink(CodexEvent.TabSelected(it)) },
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                        labelStyle = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.Small),
                        itemHeight = 36.dp,
                        itemSpacing = 2.dp,
                    )
                    CodexGroupRow(state, modifier = Modifier.padding(top = 10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    CodexListArea(state, swipeHint.peekOffsetPx)
                }
            }
            SwipeHintBanner(
                visible = swipeHint.bannerVisible,
                direction = state.swipeHintDirection,
                title = codexSwipeHintTitle(state.swipeHintDirection),
                subtitle = codexSwipeHintSubtitle(state.swipeHintDirection),
                dismissLabel = stringResource(Res.string.swipe_hint_dismiss),
                onDismiss = swipeHint::dismiss,
                modifier = floatingBarAnchor(),
            )
        }
    }

    state.deleteWarning?.let { CodexDeleteWarningDialog(it, state.eventSink) }

    when (val sheet = state.activeSheet) {
        is CodexSheet.EntryForm ->
            CodexEntryFormSheet(sheet.form, state.toast?.text) { state.eventSink(CodexEvent.EntryForm(it)) }
        is CodexSheet.EntityView -> CodexEntitySheet(state, sheet.entity)
        null -> Unit
    }
}

@Composable
private fun codexSwipeHintTitle(direction: SwipeHintDirection): String =
    when (direction) {
        SwipeHintDirection.EDIT_AND_DELETE -> stringResource(Res.string.swipe_hint_codex_both_title)
        SwipeHintDirection.DELETE_ONLY -> stringResource(Res.string.swipe_hint_delete_only_title)
    }

@Composable
private fun codexSwipeHintSubtitle(direction: SwipeHintDirection): String =
    when (direction) {
        SwipeHintDirection.EDIT_AND_DELETE -> stringResource(Res.string.swipe_hint_codex_both_subtitle)
        SwipeHintDirection.DELETE_ONLY -> stringResource(Res.string.swipe_hint_codex_quick_subtitle)
    }

@Composable
private fun CodexActiveList(
    state: CodexState,
    hintPeekOffsetPx: Float,
) {
    when (state.activeTab) {
        CodexTab.ALL -> CodexAllList(state = state, hintPeekOffsetPx = hintPeekOffsetPx)
        CodexTab.PARTY -> CodexPartyList(state = state, hintPeekOffsetPx = hintPeekOffsetPx)
        CodexTab.NPC -> CodexNpcList(state = state, hintPeekOffsetPx = hintPeekOffsetPx)
        CodexTab.QUEST -> CodexQuestList(state = state, hintPeekOffsetPx = hintPeekOffsetPx)
        CodexTab.LOCATION -> CodexLocationList(state = state, hintPeekOffsetPx = hintPeekOffsetPx)
    }
}

// the tab's list, or its empty state - a tab with no hits shows the other tabs' matches under a hint
@Composable
private fun CodexListArea(
    state: CodexState,
    hintPeekOffsetPx: Float,
) {
    when (val empty = state.emptyState) {
        CodexEmptyState.Hidden -> CodexActiveList(state, hintPeekOffsetPx)
        is CodexEmptyState.OtherSections ->
            CodexAllList(
                state = state,
                hintPeekOffsetPx = hintPeekOffsetPx,
                header = { CodexOtherSectionsHint(empty, state.eventSink) },
            )
        else -> CodexEmptyView(state)
    }
}

// the group row only shows on the tabs that group - the ALL and party tabs have no group choice
@Composable
private fun CodexGroupRow(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (state.groupOptions.isEmpty()) return
    CodexFilterChipRow(
        items = state.groupOptions,
        selected = state.groupBy,
        onSelected = { state.eventSink(CodexEvent.GroupBySelected(it)) },
        modifier = modifier,
    )
}

@Composable
private fun CodexEntitySheet(
    state: CodexState,
    entity: EntitySummaryItem,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(CodexEvent.EntitySheet(EntitySheetEvent.Dismissed)) }) {
        Box {
            EntitySummarySheetContent(
                item = entity,
                actions = entitySheetActions(state),
                onClose = { state.eventSink(CodexEvent.EntitySheet(EntitySheetEvent.Dismissed)) },
            )
            // the sheet covers the app-level undo toast, so the card carries its own copy above the bottom edge
            UndoToast(
                action = state.undoAction,
                onUndo = { state.eventSink(CodexEvent.UndoClicked) },
                onFollowUp = { state.eventSink(CodexEvent.UndoFollowUpClicked) },
                modifier = floatingBarAnchor(),
            )
        }
    }
    // a sheet of its own over the card, so dismissing it leaves the card open
    state.quickPlaceSelect?.let { select ->
        PlaceSelectSheet(
            select = select,
            onChosen = { state.eventSink(CodexEvent.QuickEdit(QuickEditUiEvent.PlacesChosen(it))) },
            onCreated = { name, checked -> state.eventSink(CodexEvent.QuickPlaceCreated(name, checked)) },
            onDismiss = { state.eventSink(CodexEvent.QuickEdit(QuickEditUiEvent.EditCancelled)) },
        )
    }
}

// quick mode edits in place and keeps the pen out of the header, form mode is read-only and offers the pen instead
private fun entitySheetActions(state: CodexState): EntitySummarySheetActions {
    val isQuick = state.editMode == EntityEditMode.QUICK
    return entitySheetActions(
        partyPresence = true,
        onEditClicked = if (isQuick) null else { ref -> state.eventSink(CodexEvent.EditEntryRequested(ref)) },
        // the swipe stays the quick way, an archived campaign has nothing to delete
        onDeleteClicked =
            if (state.readOnly) {
                null
            } else {
                { ref ->
                    state.eventSink(CodexEvent.EntityDeleteRequested(ref))
                }
            },
        statusesReadOnly = !isQuick,
        quickEdit = if (isQuick) quickEditSheet(state) else null,
    ) { state.eventSink(CodexEvent.EntitySheet(it)) }
}

private fun quickEditSheet(state: CodexState): QuickEditSheet =
    QuickEditSheet(
        inlineEdit = state.inlineEdit,
        photoCrop = state.photoCrop,
        holdTipVisible = state.holdTipVisible,
        onEvent = { event -> state.eventSink(CodexEvent.QuickEdit(event)) },
    )

@Composable
private fun codexTabItems(state: CodexState): List<SegmentedTab<CodexTab>> =
    listOf(
        SegmentedTab(
            CodexTab.ALL,
            stringResource(Res.string.codex_filter_all),
            state.tabCounts.party + state.tabCounts.npc + state.tabCounts.quest + state.tabCounts.location,
        ),
        SegmentedTab(CodexTab.PARTY, stringResource(Res.string.codex_tab_party), state.tabCounts.party),
        SegmentedTab(CodexTab.NPC, stringResource(Res.string.codex_tab_npc), state.tabCounts.npc),
        SegmentedTab(CodexTab.QUEST, stringResource(Res.string.codex_tab_quest), state.tabCounts.quest),
        SegmentedTab(CodexTab.LOCATION, stringResource(Res.string.codex_tab_location), state.tabCounts.location),
    )

// the record is still mentioned by notes - those mentions stay in the text as "?" tags, and Undo brings the record back
@Composable
private fun CodexDeleteWarningDialog(
    warning: CodexDeleteWarning,
    eventSink: (CodexEvent) -> Unit,
) {
    val mentions = pluralStringResource(Res.plurals.mention_count_mentions, warning.notes, warning.notes)
    val sessions = pluralStringResource(Res.plurals.mention_count_in_sessions, warning.sessions, warning.sessions)
    ConfirmationDialog(
        title = stringResource(Res.string.codex_delete_warning_title, warning.name),
        text = stringResource(Res.string.codex_delete_warning_text, mentions, sessions),
        onConfirm = { eventSink(CodexEvent.EntityDeleteConfirmed) },
        onDismiss = { eventSink(CodexEvent.EntityDeleteCancelled) },
        confirmLabel = stringResource(Res.string.codex_delete_warning_confirm),
        dismissLabel = stringResource(Res.string.codex_delete_warning_cancel),
    )
}
