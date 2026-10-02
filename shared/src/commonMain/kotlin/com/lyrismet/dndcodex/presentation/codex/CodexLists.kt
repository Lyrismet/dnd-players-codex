package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_delete_content_description
import dndplayerscodex.shared.generated.resources.codex_empty_filtered
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(horizontal = 16.dp, vertical = 2.dp)

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
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyStatePlaceholder(text = stringResource(Res.string.codex_empty_filtered))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        header?.let { renderHeader -> item { renderHeader() } }
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
        header = {
            CodexFilterChipRow(
                items = state.npcFilterOptions,
                selected = state.npcStatusFilter,
                onSelected = { state.eventSink(CodexEvent.NpcStatusFilterSelected(it)) },
            )
        },
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
        header = {
            CodexFilterChipRow(
                items = state.questFilterOptions,
                selected = state.questStatusFilter,
                onSelected = { state.eventSink(CodexEvent.QuestStatusFilterSelected(it)) },
            )
        },
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
        header = {
            CodexFilterChipRow(items = state.locationFilterOptions, selected = null, onSelected = {})
        },
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.npcs.isNotEmpty()) {
            item { CodexSectionHeader(stringResource(Res.string.codex_tab_npc)) }
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
            item { CodexSectionHeader(stringResource(Res.string.codex_tab_quest)) }
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
            item { CodexSectionHeader(stringResource(Res.string.codex_tab_location)) }
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
