package com.lyrismet.dndcodex.presentation.npcdetail

import com.lyrismet.dndcodex.domain.model.Npc
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class NpcDetailScreen(
    val npcId: Long,
) : Screen

data class NpcDetailState(
    val npc: Npc? = null,
    val isLoading: Boolean = true,
    val eventSink: (NpcDetailEvent) -> Unit = {},
) : CircuitUiState

sealed interface NpcDetailEvent : CircuitUiEvent {
    data object BackClicked : NpcDetailEvent
}
