package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.incadent.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_add_entry_button
import dndplayerscodex.shared.generated.resources.codex_filter_all
import dndplayerscodex.shared.generated.resources.codex_overline
import dndplayerscodex.shared.generated.resources.codex_party_empty
import dndplayerscodex.shared.generated.resources.codex_search_placeholder
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import dndplayerscodex.shared.generated.resources.codex_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun CodexUi(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    val tabItems = codexTabItems(state)

    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
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
                CodexFilters(state, modifier = Modifier.padding(top = 10.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                when (state.activeTab) {
                    CodexTab.ALL ->
                        CodexAllList(state = state)

                    CodexTab.PARTY ->
                        EmptyStatePlaceholder(text = stringResource(Res.string.codex_party_empty))

                    CodexTab.NPC ->
                        CodexNpcList(state = state)

                    CodexTab.QUEST ->
                        CodexQuestList(state = state)

                    CodexTab.LOCATION ->
                        CodexLocationList(state = state)
                }
            }
        }
    }

    when (val sheet = state.activeSheet) {
        is CodexSheet.EntryForm -> CodexEntryFormSheet(state, sheet.form)
        is CodexSheet.EntityView -> CodexEntitySheet(state, sheet.entity)
        null -> Unit
    }
}

// only the NPC and quest tabs have statuses to filter by, party and places have none
@Composable
private fun CodexFilters(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    when (state.activeTab) {
        CodexTab.NPC ->
            CodexFilterChipRow(
                items = state.npcFilterOptions,
                selected = state.npcStatusFilter,
                onSelected = { state.eventSink(CodexEvent.NpcStatusFilterSelected(it)) },
                modifier = modifier,
            )

        CodexTab.QUEST ->
            CodexFilterChipRow(
                items = state.questFilterOptions,
                selected = state.questStatusFilter,
                onSelected = { state.eventSink(CodexEvent.QuestStatusFilterSelected(it)) },
                modifier = modifier,
            )

        CodexTab.ALL, CodexTab.PARTY, CodexTab.LOCATION -> Unit
    }
}

@Composable
private fun CodexEntryFormSheet(
    state: CodexState,
    form: CodexEntryFormState,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(CodexEvent.EntryFormClosed) }) {
        CodexEntryFormUi(
            form = form,
            eventSink = state.eventSink,
            onClose = { state.eventSink(CodexEvent.EntryFormClosed) },
        )
    }
}

@Composable
private fun CodexEntitySheet(
    state: CodexState,
    entity: EntitySummaryItem,
) {
    AppBottomSheet(onDismissRequest = { state.eventSink(CodexEvent.SheetDismissed) }) {
        EntitySummarySheetContent(
            item = entity,
            actions =
                EntitySummarySheetActions(
                    onEntityRefClicked = { ref -> state.eventSink(CodexEvent.EntityClicked(ref)) },
                    onNpcStatusSelected = { id, status -> state.eventSink(CodexEvent.NpcStatusSelected(id, status)) },
                    onQuestStatusSelected = { id, status ->
                        state.eventSink(CodexEvent.QuestStatusSelected(id, status))
                    },
                    onRelatedNoteClicked = { id -> state.eventSink(CodexEvent.RelatedNoteClicked(id)) },
                    onEditClicked = { ref -> state.eventSink(CodexEvent.EditEntryRequested(ref)) },
                ),
            onClose = { state.eventSink(CodexEvent.SheetDismissed) },
        )
    }
}

@Composable
private fun codexTabItems(state: CodexState): List<CodexTabRowItem> =
    listOf(
        CodexTabRowItem(
            CodexTab.ALL,
            stringResource(Res.string.codex_filter_all),
            state.tabCounts.npc + state.tabCounts.quest + state.tabCounts.location,
        ),
        CodexTabRowItem(CodexTab.PARTY, stringResource(Res.string.codex_tab_party), state.tabCounts.party),
        CodexTabRowItem(CodexTab.NPC, stringResource(Res.string.codex_tab_npc), state.tabCounts.npc),
        CodexTabRowItem(CodexTab.QUEST, stringResource(Res.string.codex_tab_quest), state.tabCounts.quest),
        CodexTabRowItem(CodexTab.LOCATION, stringResource(Res.string.codex_tab_location), state.tabCounts.location),
    )
