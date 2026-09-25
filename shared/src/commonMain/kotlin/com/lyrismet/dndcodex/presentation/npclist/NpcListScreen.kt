package com.lyrismet.dndcodex.presentation.npclist

import com.lyrismet.dndcodex.domain.model.Npc
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object NpcListScreen : Screen

data class NpcListState(
    val npcs: List<Npc> = emptyList(),
    val eventSink: (NpcListEvent) -> Unit = {},
) : CircuitUiState

sealed interface NpcListEvent : CircuitUiEvent {
    data class NpcClicked(
        val npcId: Long,
    ) : NpcListEvent

    data object AddSampleNpcClicked : NpcListEvent
}
