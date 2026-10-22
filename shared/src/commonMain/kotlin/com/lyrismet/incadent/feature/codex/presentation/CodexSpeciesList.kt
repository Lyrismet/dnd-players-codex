package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_npc_mode_named
import dndplayerscodex.shared.generated.resources.codex_npc_mode_species
import dndplayerscodex.shared.generated.resources.codex_search_empty_reset
import dndplayerscodex.shared.generated.resources.codex_search_empty_text
import dndplayerscodex.shared.generated.resources.codex_search_empty_title
import dndplayerscodex.shared.generated.resources.codex_species_add
import dndplayerscodex.shared.generated.resources.codex_species_empty_text
import dndplayerscodex.shared.generated.resources.codex_species_empty_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CodexNpcModeSwitch(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(
                    NpcListMode.NAMED,
                    "${MentionGlyph.NPC.symbol} ${stringResource(Res.string.codex_npc_mode_named)}",
                    state.tabCounts.npc,
                ),
                SegmentedTab(
                    NpcListMode.SPECIES,
                    "${MentionGlyph.SPECIES.symbol} ${stringResource(Res.string.codex_npc_mode_species)}",
                    state.tabCounts.species,
                ),
            ),
        selected = state.npcMode,
        onSelected = { state.eventSink(CodexEvent.NpcModeSelected(it)) },
        modifier = modifier,
        labelStyle = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.Small),
        itemHeight = 34.dp,
    )
}

@Composable
internal fun SpeciesRow(
    species: SpeciesCodexItem,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
) {
    CodexDeletableRow(
        ref = EntityRef.Species(species.id),
        eventSink = eventSink,
        swipeEditEnabled = swipeEditEnabled,
    ) {
        SpeciesCodexCard(
            item = species,
            onClick = {
                eventSink(CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Species(species.id))))
            },
        )
    }
}

@Composable
internal fun CodexSpeciesList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    when {
        state.species.isNotEmpty() ->
            CodexLazyColumn(modifier) {
                items(state.species, key = { it.id }) { species ->
                    SpeciesRow(species, state.eventSink, state.swipeEditEnabled)
                }
            }
        state.tabCounts.species == 0 ->
            EmptyState(
                title = stringResource(Res.string.codex_species_empty_title),
                text = stringResource(Res.string.codex_species_empty_text),
                modifier = modifier,
                primaryAction =
                    EmptyStateAction(stringResource(Res.string.codex_species_add)) {
                        state.eventSink(CodexEvent.EmptyActionClicked(EntityKind.SPECIES, ""))
                    },
            )
        else ->
            EmptyState(
                title = stringResource(Res.string.codex_search_empty_title),
                text = stringResource(Res.string.codex_search_empty_text, state.searchQuery.trim()),
                modifier = modifier,
                primaryAction =
                    EmptyStateAction(stringResource(Res.string.codex_search_empty_reset)) {
                        state.eventSink(CodexEvent.SearchQueryChanged(""))
                    },
            )
    }
}
