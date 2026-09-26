package com.lyrismet.dndcodex.presentation.sessionlist

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object SessionListScreen : Screen

data class SessionListState(
    val campaignName: String = "",
    val sessions: List<SessionListItem> = emptyList(),
    val eventSink: (SessionListEvent) -> Unit = {},
) : CircuitUiState

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class SessionListItem(
    val id: Long,
    val numberLabel: String,
    val title: String,
    val dateLabel: String,
)

sealed interface SessionListEvent : CircuitUiEvent {
    data object NewSessionClicked : SessionListEvent

    data class SessionClicked(
        val id: Long,
    ) : SessionListEvent

    data class DeleteSessionClicked(
        val id: Long,
    ) : SessionListEvent
}
