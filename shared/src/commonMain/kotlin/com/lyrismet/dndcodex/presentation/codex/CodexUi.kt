package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.ScreenHeader
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
    val tabItems =
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
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
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
}
