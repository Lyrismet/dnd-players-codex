package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SwipeHintBanner
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.rememberSwipeHintPlayback
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import com.lyrismet.incadent.core.entitysummary.QuickEditSheet
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.domain.model.EntityEditMode
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_add_entry_button
import dndplayerscodex.shared.generated.resources.codex_filter_all
import dndplayerscodex.shared.generated.resources.codex_overline
import dndplayerscodex.shared.generated.resources.codex_search_placeholder
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import dndplayerscodex.shared.generated.resources.codex_title
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_both_subtitle
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_both_title
import dndplayerscodex.shared.generated.resources.swipe_hint_codex_quick_subtitle
import dndplayerscodex.shared.generated.resources.swipe_hint_delete_only_title
import dndplayerscodex.shared.generated.resources.swipe_hint_dismiss
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
                        HeaderActionButton(
                            text = stringResource(Res.string.codex_add_entry_button),
                            onClick = { state.eventSink(CodexEvent.AddEntryClicked) },
                        )
                    },
                )
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    CodexSearchField(
                        query = state.searchQuery,
                        onQueryChange = { state.eventSink(CodexEvent.SearchQueryChanged(it)) },
                        placeholder = stringResource(Res.string.codex_search_placeholder),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    CodexTabRow(
                        items = tabItems,
                        selected = state.activeTab,
                        onSelected = { state.eventSink(CodexEvent.TabSelected(it)) },
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                    )
                    CodexGroupRow(state, modifier = Modifier.padding(top = 10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (state.emptyState == CodexEmptyState.Hidden) {
                        CodexActiveList(state, swipeHint.peekOffsetPx)
                    } else {
                        CodexEmptyView(state)
                    }
                }
            }
            SwipeHintBanner(
                visible = swipeHint.bannerVisible,
                direction = state.swipeHintDirection,
                title = codexSwipeHintTitle(state.swipeHintDirection),
                subtitle = codexSwipeHintSubtitle(state.swipeHintDirection),
                dismissLabel = stringResource(Res.string.swipe_hint_dismiss),
                onDismiss = swipeHint::dismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }

    when (val sheet = state.activeSheet) {
        is CodexSheet.EntryForm -> CodexEntryFormSheet(state, sheet.form)
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
private fun CodexEntryFormSheet(
    state: CodexState,
    form: CodexEntryFormState,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(CodexEvent.EntryFormClosed) }) {
        // mutually exclusive, like the mockup's isPadSheet/isNewSheet sc-if branches - never both, so the
        // sheet's height always tracks whichever one is actually shown instead of the taller form underneath
        val calculator = form.calculator
        if (calculator != null) {
            NumberPadSheet(
                overline = calculator.overline,
                title = calculator.title,
                expr = calculator.expr,
                result = calculator.result,
                rangeHint = calculator.rangeHint,
                applyLabel = calculator.applyLabel,
                applyEnabled = calculator.applyEnabled,
                onDigit = { state.eventSink(CodexEvent.CalculatorDigitPressed(it)) },
                onBackspace = { state.eventSink(CodexEvent.CalculatorBackspacePressed) },
                onClear = { state.eventSink(CodexEvent.CalculatorClearPressed) },
                onApply = { state.eventSink(CodexEvent.CalculatorApplyClicked) },
                onClose = { state.eventSink(CodexEvent.CalculatorClosed) },
            )
        } else {
            CodexEntryFormUi(
                form = form,
                eventSink = state.eventSink,
                onClose = { state.eventSink(CodexEvent.EntryFormClosed) },
            )
        }
    }
}

@Composable
private fun CodexEntitySheet(
    state: CodexState,
    entity: EntitySummaryItem,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(CodexEvent.SheetDismissed) }) {
        Box {
            EntitySummarySheetContent(
                item = entity,
                actions = entitySheetActions(state),
                onClose = { state.eventSink(CodexEvent.SheetDismissed) },
            )
            // the sheet covers the app-level undo toast, so the card carries its own copy above the bottom edge
            UndoToast(
                action = state.undoAction,
                onUndo = { state.eventSink(CodexEvent.UndoClicked) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

// quick mode edits in place and keeps the pen out of the header, form mode is read-only and offers the pen instead
private fun entitySheetActions(state: CodexState): EntitySummarySheetActions {
    val isQuick = state.editMode == EntityEditMode.QUICK
    return EntitySummarySheetActions(
        onEntityRefClicked = { ref -> state.eventSink(CodexEvent.EntityClicked(ref)) },
        onNpcStatusSelected = { id, status -> state.eventSink(CodexEvent.NpcStatusSelected(id, status)) },
        onNpcLifeSelected = { id, life -> state.eventSink(CodexEvent.NpcLifeSelected(id, life)) },
        onPartyPresenceSelected = { id, presence -> state.eventSink(CodexEvent.PartyPresenceSelected(id, presence)) },
        onQuestStatusSelected = { id, status -> state.eventSink(CodexEvent.QuestStatusSelected(id, status)) },
        onRelatedNoteClicked = { id, entryId -> state.eventSink(CodexEvent.RelatedNoteClicked(id, entryId)) },
        onEditClicked = if (isQuick) null else { ref -> state.eventSink(CodexEvent.EditEntryRequested(ref)) },
        statusesReadOnly = !isQuick,
        quickEdit = if (isQuick) quickEditSheet(state) else null,
    )
}

private fun quickEditSheet(state: CodexState): QuickEditSheet =
    QuickEditSheet(
        inlineEdit = state.inlineEdit,
        holdTipVisible = state.holdTipVisible,
        onEvent = { event -> state.eventSink(CodexEvent.QuickEdit(event)) },
    )

@Composable
private fun codexTabItems(state: CodexState): List<CodexTabRowItem> =
    listOf(
        CodexTabRowItem(
            CodexTab.ALL,
            stringResource(Res.string.codex_filter_all),
            state.tabCounts.party + state.tabCounts.npc + state.tabCounts.quest + state.tabCounts.location,
        ),
        CodexTabRowItem(CodexTab.PARTY, stringResource(Res.string.codex_tab_party), state.tabCounts.party),
        CodexTabRowItem(CodexTab.NPC, stringResource(Res.string.codex_tab_npc), state.tabCounts.npc),
        CodexTabRowItem(CodexTab.QUEST, stringResource(Res.string.codex_tab_quest), state.tabCounts.quest),
        CodexTabRowItem(CodexTab.LOCATION, stringResource(Res.string.codex_tab_location), state.tabCounts.location),
    )
