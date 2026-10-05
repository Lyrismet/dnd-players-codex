package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.EmptyStatePlaceholder
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_empty_filtered
import dndplayerscodex.shared.generated.resources.codex_party_empty
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp)

@Composable
private fun <T> CodexEntityList(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = itemKey) { itemContent(it) }
    }
}

// one titled group inside the ALL tab - renders nothing when the group is empty, so no header is left dangling
private fun <T> LazyListScope.codexSection(
    title: String,
    items: List<T>,
    isFirst: Boolean,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    item { CodexSectionHeader(title, items.size, isFirst = isFirst) }
    items(items, key = key) { itemContent(it) }
}

@Composable
internal fun CodexPartyList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    // an empty party is a blank page to fill, a filter that hides everyone is just an empty result
    if (state.tabCounts.party == 0) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_party_empty))
        return
    }
    CodexEntityList(
        items = state.party,
        itemKey = { it.id },
        modifier = modifier,
    ) { PartyRow(member = it, eventSink = state.eventSink) }
}

@Composable
internal fun CodexNpcList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    CodexEntityList(
        items = state.npcs,
        itemKey = { it.id },
        modifier = modifier,
    ) { NpcRow(npc = it, eventSink = state.eventSink) }
}

@Composable
internal fun CodexQuestList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    CodexEntityList(
        items = state.quests,
        itemKey = { it.id },
        modifier = modifier,
    ) { QuestRow(quest = it, eventSink = state.eventSink) }
}

@Composable
internal fun CodexLocationList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    CodexEntityList(
        items = state.locations,
        itemKey = { it.id },
        modifier = modifier,
    ) { LocationRow(location = it, eventSink = state.eventSink) }
}

@Composable
internal fun CodexAllList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (listOf<List<*>>(state.party, state.npcs, state.quests, state.locations).all { it.isEmpty() }) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    // resolved here, not inside the lazy content block, which is not a composable context
    val partyTitle = stringResource(Res.string.codex_tab_party)
    val npcTitle = stringResource(Res.string.codex_tab_npc)
    val questTitle = stringResource(Res.string.codex_tab_quest)
    val locationTitle = stringResource(Res.string.codex_tab_location)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        codexSection(partyTitle, state.party, isFirst = true, key = { "party_${it.id}" }) {
            PartyRow(member = it, eventSink = state.eventSink)
        }
        codexSection(npcTitle, state.npcs, isFirst = state.party.isEmpty(), key = { "npc_${it.id}" }) {
            NpcRow(npc = it, eventSink = state.eventSink)
        }
        codexSection(
            questTitle,
            state.quests,
            isFirst = state.party.isEmpty() && state.npcs.isEmpty(),
            key = { "quest_${it.id}" },
        ) { QuestRow(quest = it, eventSink = state.eventSink) }
        codexSection(
            locationTitle,
            state.locations,
            isFirst = state.party.isEmpty() && state.npcs.isEmpty() && state.quests.isEmpty(),
            key = { "location_${it.id}" },
        ) { LocationRow(location = it, eventSink = state.eventSink) }
    }
}
