package com.lyrismet.incadent.feature.session.presentation.list

import com.lyrismet.incadent.core.swipehint.SwipeHintTarget
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object SessionListScreen : Screen

data class SessionListState(
    val campaignName: String = "",
    // a finished campaign is read-only - no new session, no delete, and the banner offers to resume it
    val campaignArchived: Boolean = false,
    val campaignId: String? = null,
    val campaignSheet: CampaignSheetState = CampaignSheetState.Hidden,
    val liveSession: LiveSessionItem? = null,
    val sessions: List<SessionListItem> = emptyList(),
    // the sessions have not arrived yet, so the list must not claim there are none
    val isLoading: Boolean = true,
    val selectedEntity: EntitySummaryItem? = null,
    // a card was tapped and its summary is still being built
    val entityLoading: Boolean = false,
    // the in-place editing of the open entity card
    val quickEdit: QuickEditState = QuickEditState(),
    val toast: ToastMessage? = null,
    // the archive row the one-time swipe-to-delete hint peeks on - always delete-only, sessions have no swipe-edit
    val swipeHintTarget: SwipeHintTarget<Long>? = null,
    val eventSink: (SessionListEvent) -> Unit = {},
) : CircuitUiState

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class SessionListItem(
    val id: Long,
    val numberLabel: String,
    val arabicNumber: Int,
    val title: String,
    val dateLabel: String,
    // the archive row's meta line is the date and this count, the live card shows its own notes summary
    val noteCount: Int,
    val tags: List<String> = emptyList(),
    // distinct @-mentioned entities (any type) across every note of the session, 0 hides the "@ N" badge
    val mentionCount: Int = 0,
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

    /** a tap or change on the entity card that opens over this screen */
    data class EntitySheet(
        val event: EntitySheetEvent,
    ) : SessionListEvent

    /** an in-place edit on the entity card that opens over this screen */
    data class QuickEdit(
        val event: QuickEditUiEvent,
    ) : SessionListEvent

    data class CampaignSheet(
        val event: CampaignSheetEvent,
    ) : SessionListEvent
}
