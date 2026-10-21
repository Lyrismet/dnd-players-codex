package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// what narrows the active tab - the named or species switch of the npc tab, the group chips of the tabs that group
@Composable
internal fun CodexListChoices(state: CodexState) {
    if (state.activeTab == CodexTab.NPC) {
        CodexNpcModeSwitch(state, modifier = Modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp))
    }
    if (!state.showsSpecies) CodexGroupRow(state, modifier = Modifier.padding(top = 10.dp))
}

// the npc tab lists the kinds of creature instead of the named npcs
internal val CodexState.showsSpecies: Boolean
    get() = activeTab == CodexTab.NPC && npcMode == NpcListMode.SPECIES

// the group row only shows on the tabs that group - the ALL and party tabs have no group choice
@Composable
private fun CodexGroupRow(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    if (state.groupOptions.isEmpty()) return
    CodexFilterChipRow(
        items = state.groupOptions,
        selected = state.groupBy,
        onSelected = { state.eventSink(CodexEvent.GroupBySelected(it)) },
        modifier = modifier,
    )
}
