package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.ScreenHeader
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_add_entry_button
import dndplayerscodex.shared.generated.resources.codex_empty_filtered
import dndplayerscodex.shared.generated.resources.codex_overline
import dndplayerscodex.shared.generated.resources.codex_party_empty
import dndplayerscodex.shared.generated.resources.codex_search_placeholder
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import dndplayerscodex.shared.generated.resources.codex_title
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(horizontal = 16.dp, vertical = 2.dp)

@Composable
fun CodexUi(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    val tabItems =
        listOf(
            CodexTabRowItem(CodexTab.PARTY, stringResource(Res.string.codex_tab_party), state.tabCounts.party),
            CodexTabRowItem(CodexTab.NPC, stringResource(Res.string.codex_tab_npc), state.tabCounts.npc),
            CodexTabRowItem(CodexTab.QUEST, stringResource(Res.string.codex_tab_quest), state.tabCounts.quest),
            CodexTabRowItem(CodexTab.LOCATION, stringResource(Res.string.codex_tab_location), state.tabCounts.location),
        )

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
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CodexSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.eventSink(CodexEvent.SearchQueryChanged(it)) },
                    placeholder = stringResource(Res.string.codex_search_placeholder),
                )
                CodexTabRow(
                    items = tabItems,
                    selected = state.activeTab,
                    onSelected = { state.eventSink(CodexEvent.TabSelected(it)) },
                )
            }
            when (state.activeTab) {
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

@Composable
private fun CodexNpcList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (state.npcs.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CodexFilterChipRow(
                items = state.npcFilterOptions,
                selected = state.npcStatusFilter,
                onSelected = { state.eventSink(CodexEvent.NpcStatusFilterSelected(it)) },
            )
        }
        items(state.npcs, key = { it.id }) { npc ->
            NpcCodexCard(item = npc, onClick = { state.eventSink(CodexEvent.NpcClicked(npc.id)) })
        }
    }
}

@Composable
private fun CodexQuestList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (state.quests.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CodexFilterChipRow(
                items = state.questFilterOptions,
                selected = state.questStatusFilter,
                onSelected = { state.eventSink(CodexEvent.QuestStatusFilterSelected(it)) },
            )
        }
        items(state.quests, key = { it.id }) { quest ->
            QuestCodexCard(item = quest, onGiverClick = { state.eventSink(CodexEvent.NpcClicked(it)) })
        }
    }
}

@Composable
private fun CodexLocationList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (state.locations.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.locations, key = { it.id }) { location ->
            LocationCodexCard(item = location)
        }
    }
}
