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
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.entitysummary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_delete_content_description
import dndplayerscodex.shared.generated.resources.codex_empty_filtered
import dndplayerscodex.shared.generated.resources.codex_party_empty
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp)

@Composable
private fun CodexDeletableRow(
    ref: EntityRef,
    eventSink: (CodexEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeToDeleteRow(
        onDeleteRequested = { eventSink(CodexEvent.EntityDeleteRequested(ref)) },
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.codex_delete_content_description),
        content = content,
    )
}

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
    ) { member ->
        CodexDeletableRow(ref = EntityRef.Party(member.id), eventSink = state.eventSink) {
            PartyCodexCard(
                item = member,
                onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Party(member.id))) },
            )
        }
    }
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
    ) { npc ->
        CodexDeletableRow(ref = EntityRef.Npc(npc.id), eventSink = state.eventSink) {
            NpcCodexCard(item = npc, onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(npc.id))) })
        }
    }
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
    ) { quest ->
        CodexDeletableRow(ref = EntityRef.Quest(quest.id), eventSink = state.eventSink) {
            QuestCodexCard(
                item = quest,
                onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Quest(quest.id))) },
                onGiverClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(it))) },
            )
        }
    }
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
    ) { location ->
        CodexDeletableRow(ref = EntityRef.Location(location.id), eventSink = state.eventSink) {
            LocationCodexCard(
                item = location,
                onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Location(location.id))) },
            )
        }
    }
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
        codexSection(partyTitle, state.party, isFirst = true, key = { "party_${it.id}" }) { member ->
            CodexDeletableRow(ref = EntityRef.Party(member.id), eventSink = state.eventSink) {
                PartyCodexCard(
                    item = member,
                    onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Party(member.id))) },
                )
            }
        }
        codexSection(npcTitle, state.npcs, isFirst = state.party.isEmpty(), key = { "npc_${it.id}" }) { npc ->
            CodexDeletableRow(ref = EntityRef.Npc(npc.id), eventSink = state.eventSink) {
                NpcCodexCard(item = npc, onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(npc.id))) })
            }
        }
        codexSection(
            questTitle,
            state.quests,
            isFirst = state.party.isEmpty() && state.npcs.isEmpty(),
            key = { "quest_${it.id}" },
        ) { quest ->
            CodexDeletableRow(ref = EntityRef.Quest(quest.id), eventSink = state.eventSink) {
                QuestCodexCard(
                    item = quest,
                    onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Quest(quest.id))) },
                    onGiverClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(it))) },
                )
            }
        }
        codexSection(
            locationTitle,
            state.locations,
            isFirst = state.party.isEmpty() && state.npcs.isEmpty() && state.quests.isEmpty(),
            key = { "location_${it.id}" },
        ) { location ->
            CodexDeletableRow(ref = EntityRef.Location(location.id), eventSink = state.eventSink) {
                LocationCodexCard(
                    item = location,
                    onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Location(location.id))) },
                )
            }
        }
    }
}
