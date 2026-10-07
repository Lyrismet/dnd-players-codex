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
import com.lyrismet.incadent.core.codexgroup.CodexGroup
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.EmptyStateActions
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.entitysummary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp)

// the codex screen only renders a list once its tab has entries - the empty states are handled by CodexEmptyView
@Composable
private fun <T> CodexEntityList(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = itemKey) { itemContent(it) }
    }
}

// a grouped list - the title header is skipped for the ungrouped "as a list" section
@Composable
private fun <T> CodexGroupedList(
    groups: List<CodexGroup<T>>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEachIndexed { index, group ->
            group.title?.let { title ->
                item {
                    CodexSectionHeader("${MentionGlyph.LOCATION.symbol} $title", group.items.size, isFirst = index == 0)
                }
            }
            items(group.items, key = itemKey) { itemContent(it) }
        }
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
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    CodexEntityList(
        items = state.party,
        itemKey = { it.id },
        modifier = modifier,
    ) {
        PartyRow(
            member = it,
            eventSink = state.eventSink,
            swipeEditEnabled = state.swipeEditEnabled,
            peekOffsetPx = state.peekOffsetPxFor(EntityRef.Party(it.id), hintPeekOffsetPx),
        )
    }
}

@Composable
internal fun CodexNpcList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    CodexGroupedList(
        groups = state.npcs,
        itemKey = { it.id },
        modifier = modifier,
    ) {
        NpcRow(
            npc = it,
            eventSink = state.eventSink,
            swipeEditEnabled = state.swipeEditEnabled,
            peekOffsetPx = state.peekOffsetPxFor(EntityRef.Npc(it.id), hintPeekOffsetPx),
        )
    }
}

@Composable
internal fun CodexQuestList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    CodexGroupedList(
        groups = state.quests,
        itemKey = { it.id },
        modifier = modifier,
    ) {
        QuestRow(
            quest = it,
            eventSink = state.eventSink,
            swipeEditEnabled = state.swipeEditEnabled,
            peekOffsetPx = state.peekOffsetPxFor(EntityRef.Quest(it.id), hintPeekOffsetPx),
        )
    }
}

@Composable
internal fun CodexLocationList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    CodexGroupedList(
        groups = state.locations,
        itemKey = { it.id },
        modifier = modifier,
    ) {
        LocationRow(
            location = it,
            eventSink = state.eventSink,
            swipeEditEnabled = state.swipeEditEnabled,
            peekOffsetPx = state.peekOffsetPxFor(EntityRef.Location(it.id), hintPeekOffsetPx),
        )
    }
}

@Composable
internal fun CodexAllList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    // the ALL tab never groups, so every group holds the one ungrouped list and flattening it is lossless
    val npcs = state.npcs.flatMap { it.items }
    val quests = state.quests.flatMap { it.items }
    val locations = state.locations.flatMap { it.items }
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
            PartyRow(
                member = it,
                eventSink = state.eventSink,
                swipeEditEnabled = state.swipeEditEnabled,
                peekOffsetPx = state.peekOffsetPxFor(EntityRef.Party(it.id), hintPeekOffsetPx),
            )
        }
        codexSection(npcTitle, npcs, isFirst = state.party.isEmpty(), key = { "npc_${it.id}" }) {
            NpcRow(
                npc = it,
                eventSink = state.eventSink,
                swipeEditEnabled = state.swipeEditEnabled,
                peekOffsetPx = state.peekOffsetPxFor(EntityRef.Npc(it.id), hintPeekOffsetPx),
            )
        }
        codexSection(
            questTitle,
            quests,
            isFirst = state.party.isEmpty() && npcs.isEmpty(),
            key = { "quest_${it.id}" },
        ) {
            QuestRow(
                quest = it,
                eventSink = state.eventSink,
                swipeEditEnabled = state.swipeEditEnabled,
                peekOffsetPx = state.peekOffsetPxFor(EntityRef.Quest(it.id), hintPeekOffsetPx),
            )
        }
        codexSection(
            locationTitle,
            locations,
            isFirst = state.party.isEmpty() && npcs.isEmpty() && quests.isEmpty(),
            key = { "location_${it.id}" },
        ) {
            LocationRow(
                location = it,
                eventSink = state.eventSink,
                swipeEditEnabled = state.swipeEditEnabled,
                peekOffsetPx = state.peekOffsetPxFor(EntityRef.Location(it.id), hintPeekOffsetPx),
            )
        }
    }
}

// only the row the swipe hint targets actually peeks - every other row stays at rest
private fun CodexState.peekOffsetPxFor(
    ref: EntityRef,
    hintPeekOffsetPx: Float,
): Float = if (swipeHintTarget?.itemId == ref) hintPeekOffsetPx else 0f

// a blank tab offers its create button, a search with no hits offers a clear and a create when the query is long enough
@Composable
internal fun CodexEmptyView(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    when (val empty = state.emptyState) {
        CodexEmptyState.Hidden -> Unit
        is CodexEmptyState.Blank ->
            EmptyStateActions(
                title = empty.title,
                text = empty.text,
                modifier = modifier,
                primaryAction = empty.action.toEmptyStateAction(state.eventSink),
            )

        is CodexEmptyState.NoMatch ->
            EmptyStateActions(
                title = empty.title,
                text = empty.text,
                modifier = modifier,
                primaryAction = empty.create?.toEmptyStateAction(state.eventSink),
                secondaryAction =
                    EmptyStateAction(label = empty.resetLabel) {
                        state.eventSink(CodexEvent.SearchQueryChanged(""))
                    },
            )
    }
}

private fun CodexEmptyAction.toEmptyStateAction(eventSink: (CodexEvent) -> Unit): EmptyStateAction =
    EmptyStateAction(label = label) { eventSink(CodexEvent.EmptyActionClicked(entryType, name)) }
