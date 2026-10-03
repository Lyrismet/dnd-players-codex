package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
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
    if (state.npcs.isEmpty() && state.quests.isEmpty() && state.locations.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.npcs.isNotEmpty()) {
            item { CodexSectionHeader(stringResource(Res.string.codex_tab_npc), state.npcs.size, isFirst = true) }
            items(state.npcs, key = { "npc_${it.id}" }) { npc ->
                CodexDeletableRow(ref = EntityRef.Npc(npc.id), eventSink = state.eventSink) {
                    NpcCodexCard(
                        item = npc,
                        onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(npc.id))) },
                    )
                }
            }
        }
        if (state.quests.isNotEmpty()) {
            item {
                CodexSectionHeader(
                    stringResource(Res.string.codex_tab_quest),
                    state.quests.size,
                    isFirst = state.npcs.isEmpty(),
                )
            }
            items(state.quests, key = { "quest_${it.id}" }) { quest ->
                CodexDeletableRow(ref = EntityRef.Quest(quest.id), eventSink = state.eventSink) {
                    QuestCodexCard(
                        item = quest,
                        onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Quest(quest.id))) },
                        onGiverClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Npc(it))) },
                    )
                }
            }
        }
        if (state.locations.isNotEmpty()) {
            item {
                CodexSectionHeader(
                    stringResource(Res.string.codex_tab_location),
                    state.locations.size,
                    isFirst = state.npcs.isEmpty() && state.quests.isEmpty(),
                )
            }
            items(state.locations, key = { "location_${it.id}" }) { location ->
                CodexDeletableRow(ref = EntityRef.Location(location.id), eventSink = state.eventSink) {
                    LocationCodexCard(
                        item = location,
                        onClick = { state.eventSink(CodexEvent.EntityClicked(EntityRef.Location(location.id))) },
                    )
                }
            }
        }
    }
}
