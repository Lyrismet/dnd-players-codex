package com.lyrismet.dndcodex.presentation.sessiondetail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class SessionDetailScreen(
    val sessionNoteId: Long,
) : Screen

data class SessionDetailState(
    val isLoading: Boolean = true,
    val overline: String = "",
    val title: String = "",
    val dateLabel: String = "",
    val isLive: Boolean = true,
    val entries: List<SessionEntryItem> = emptyList(),
    val draft: String = "",
    val eventSink: (SessionDetailEvent) -> Unit = {},
) : CircuitUiState

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class SessionEntryItem(
    val id: Long,
    val timeLabel: String,
    val body: String,
)

sealed interface SessionDetailEvent : CircuitUiEvent {
    data object BackClicked : SessionDetailEvent

    data class TitleChanged(
        val title: String,
    ) : SessionDetailEvent

    data class DraftChanged(
        val text: String,
    ) : SessionDetailEvent

    data object SubmitEntryClicked : SessionDetailEvent

    data class DeleteEntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object EndSessionClicked : SessionDetailEvent

    data object ResumeSessionClicked : SessionDetailEvent
}
