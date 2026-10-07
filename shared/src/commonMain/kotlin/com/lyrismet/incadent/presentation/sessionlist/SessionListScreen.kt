package com.lyrismet.incadent.presentation.sessionlist

import com.lyrismet.incadent.core.campaign.RenameSheetState
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object SessionListScreen : Screen

data class SessionListState(
    val campaignName: String = "",
    val liveSession: LiveSessionItem? = null,
    val sessions: List<SessionListItem> = emptyList(),
    val selectedEntity: EntitySummaryItem? = null,
    val renameSheet: RenameSheetState = RenameSheetState.Hidden,
    val toast: SessionListToast? = null,
    val eventSink: (SessionListEvent) -> Unit = {},
) : CircuitUiState

/** compared by identity, so repeating the same message restarts its timer */
class SessionListToast(
    val text: String,
)

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class SessionListItem(
    val id: Long,
    val numberLabel: String,
    val arabicNumber: Int,
    val title: String,
    val dateLabel: String,
)

/** the "currently running" session card shown above the archive - tapping it resumes note-taking */
data class LiveSessionItem(
    val id: Long,
    val numberLabel: String,
    val overline: String,
    val title: String,
    val dateLabel: String,
    val notesSummary: String,
    val mentions: List<MentionChipItem>,
)

sealed interface SessionListEvent : CircuitUiEvent {
    data object NewSessionClicked : SessionListEvent

    data class SessionClicked(
        val id: Long,
    ) : SessionListEvent

    data class DeleteSessionClicked(
        val id: Long,
    ) : SessionListEvent

    data class MentionChipClicked(
        val ref: EntityRef,
    ) : SessionListEvent

    data class NpcStatusSelected(
        val npcId: Long,
        val status: NpcStatus,
    ) : SessionListEvent

    data class NpcLifeSelected(
        val npcId: Long,
        val lifeState: NpcLifeState,
    ) : SessionListEvent

    data class QuestStatusSelected(
        val questId: Long,
        val status: QuestStatus,
    ) : SessionListEvent

    data class RelatedNoteClicked(
        val sessionNoteId: Long,
        val entryId: Long,
    ) : SessionListEvent

    data object SheetDismissed : SessionListEvent

    data object RenameOpened : SessionListEvent

    data class RenameDraftChanged(
        val value: String,
    ) : SessionListEvent

    data object RenameSaved : SessionListEvent

    data object RenameDismissed : SessionListEvent
}
