package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.ListSectionHeader
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroup
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_all_species
import dndplayerscodex.shared.generated.resources.codex_party_fallen_section
import dndplayerscodex.shared.generated.resources.codex_tab_location
import dndplayerscodex.shared.generated.resources.codex_tab_npc
import dndplayerscodex.shared.generated.resources.codex_tab_party
import dndplayerscodex.shared.generated.resources.codex_tab_quest
import org.jetbrains.compose.resources.stringResource

private val LIST_CONTENT_PADDING = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp)

// every codex list shares the padding and the 8dp rhythm
@Composable
internal fun CodexLazyColumn(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = LIST_CONTENT_PADDING,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

// a grouped list - the title header is skipped for the ungrouped "as a list" section
@Composable
private fun <T> CodexGroupedList(
    groups: List<CodexGroup<T>>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    onSectionToggled: (Long) -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    CodexLazyColumn(modifier) {
        groups.forEachIndexed { index, group ->
            val toggleId = group.toggleId
            if (toggleId != null && group.title != null) {
                item(key = "section-$toggleId") {
                    CodexPlaceSectionHeader(
                        title = group.title,
                        path = group.path,
                        count = group.count,
                        collapsed = group.collapsed,
                        onToggle = { onSectionToggled(toggleId) },
                    )
                }
            } else {
                group.title?.let { title ->
                    item {
                        ListSectionHeader(
                            text = "${MentionGlyph.LOCATION.symbol} $title",
                            modifier = Modifier.padding(top = if (index == 0) 4.dp else 12.dp),
                            count = group.items.size,
                        )
                    }
                }
            }
            // an entity met in several places repeats across sections, so the section is part of the key
            items(group.items, key = { "$index-${itemKey(it)}" }) { itemContent(it) }
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
    item {
        ListSectionHeader(
            text = title,
            modifier = Modifier.padding(top = if (isFirst) 4.dp else 12.dp),
            count = items.size,
        )
    }
    items(items, key = key) { itemContent(it) }
}

@Composable
internal fun CodexPartyList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    CodexGroupedList(
        groups = rememberPartyGroups(state.party),
        itemKey = { it.id },
        modifier = modifier,
    ) { CodexPartyEntry(state, it, hintPeekOffsetPx) }
}

// the living members first, the fallen under their own heading
@Composable
private fun rememberPartyGroups(party: List<PartyCodexItem>): List<CodexGroup<PartyCodexItem>> {
    val fallenTitle = stringResource(Res.string.codex_party_fallen_section)
    return remember(party, fallenTitle) {
        val (fallen, living) = party.partition { it.fallen }
        listOfNotNull(
            CodexGroup(title = null, items = living),
            fallen.takeIf { it.isNotEmpty() }?.let { CodexGroup(title = fallenTitle, items = it) },
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
    ) { CodexNpcEntry(state, it, hintPeekOffsetPx) }
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
    ) { CodexQuestEntry(state, it, hintPeekOffsetPx) }
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
        onSectionToggled = { state.eventSink(CodexEvent.PlaceSectionToggled(it)) },
    ) { CodexLocationEntry(state, it, hintPeekOffsetPx) }
}

@Composable
internal fun CodexAllList(
    state: CodexState,
    hintPeekOffsetPx: Float,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    // the ALL tab never groups, so every group holds the one ungrouped list and flattening it is lossless
    val npcs = state.npcs.flatMap { it.items }
    val quests = state.quests.flatMap { it.items }
    val locations = state.locations.flatMap { it.items }
    // the first section that has entries sits closer to the top, resolved by what comes before it
    val npcFirst = state.party.isEmpty()
    val speciesFirst = npcFirst && npcs.isEmpty()
    val questFirst = speciesFirst && state.species.isEmpty()
    val locationFirst = questFirst && quests.isEmpty()
    // resolved here, not inside the lazy content block, which is not a composable context
    val partyTitle = stringResource(Res.string.codex_tab_party)
    val npcTitle = stringResource(Res.string.codex_tab_npc)
    val speciesTitle = stringResource(Res.string.codex_all_species)
    val questTitle = stringResource(Res.string.codex_tab_quest)
    val locationTitle = stringResource(Res.string.codex_tab_location)
    CodexLazyColumn(modifier) {
        header?.let { item(key = "header") { it() } }
        codexSection(partyTitle, state.party, isFirst = true, key = { "party_${it.id}" }) {
            CodexPartyEntry(state, it, hintPeekOffsetPx)
        }
        codexSection(npcTitle, npcs, npcFirst, key = { "npc_${it.id}" }) {
            CodexNpcEntry(state, it, hintPeekOffsetPx)
        }
        codexSection(speciesTitle, state.species, speciesFirst, key = { "species_${it.id}" }) { species ->
            SpeciesRow(species, state.eventSink, state.swipeEditEnabled)
        }
        codexSection(questTitle, quests, questFirst, key = { "quest_${it.id}" }) {
            CodexQuestEntry(state, it, hintPeekOffsetPx)
        }
        codexSection(locationTitle, locations, locationFirst, key = { "location_${it.id}" }) {
            CodexLocationEntry(state, it, hintPeekOffsetPx)
        }
    }
}

// a blank tab offers its create button, a search with no hits offers a clear and a create when the query is long enough
@Composable
internal fun CodexEmptyView(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    when (val empty = state.emptyState) {
        CodexEmptyState.Hidden -> Unit
        is CodexEmptyState.Blank ->
            EmptyState(
                title = empty.title,
                text = empty.text,
                modifier = modifier,
                primaryAction = empty.action.toEmptyStateAction(state.eventSink),
            )

        // rendered above the list by the screen, never as an empty view
        is CodexEmptyState.OtherSections -> Unit

        is CodexEmptyState.NoMatch ->
            EmptyState(
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
