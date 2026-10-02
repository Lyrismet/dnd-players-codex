package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.lyrismet.dndcodex.core.designsystem.component.MentionChipItem
import com.lyrismet.dndcodex.core.designsystem.component.MentionGlyph
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import com.lyrismet.dndcodex.core.entitysummary.EntitySummaryItem
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus
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
    val headerMentions: List<MentionChipItem> = emptyList(),
    val feed: List<SessionFeedItem> = emptyList(),
    val draft: String = "",
    val draftSelection: TextRange = TextRange.Zero,
    val mentionSuggestions: List<SessionMentionSuggestion> = emptyList(),
    val selectedEntity: EntitySummaryItem? = null,
    val editingEntryTimeLabel: String? = null,
    val selectedEntryId: Long? = null,
    val editingEntryId: Long? = null,
    val eventSink: (SessionDetailEvent) -> Unit = {},
) : CircuitUiState {
    val isEditingEntry: Boolean get() = editingEntryTimeLabel != null
}

/** one row in the note feed - either a meeting-day separator (multi-day sessions only) or a timestamped note */
sealed interface SessionFeedItem {
    val key: String

    data class DaySeparator(
        override val key: String,
        val label: String,
    ) : SessionFeedItem

    data class Note(
        val entry: SessionEntryItem,
    ) : SessionFeedItem {
        override val key: String get() = "note-${entry.id}"
    }
}

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class SessionEntryItem(
    val id: Long,
    val timeLabel: String,
    val segments: List<SessionEntrySegment>,
)

sealed interface SessionEntrySegment {
    data class Text(
        val text: String,
    ) : SessionEntrySegment

    data class Mention(
        val chip: MentionChipItem,
    ) : SessionEntrySegment
}

data class SessionMentionSuggestion(
    val candidateKey: String,
    val glyph: MentionGlyph,
    val tint: Color,
    val name: String,
    val typeLabel: String,
)

sealed interface SessionDetailEvent : CircuitUiEvent {
    data object BackClicked : SessionDetailEvent

    data class TitleChanged(
        val title: String,
    ) : SessionDetailEvent

    data class DraftChanged(
        val text: String,
        val selection: TextRange,
    ) : SessionDetailEvent

    data object InsertMentionTriggerClicked : SessionDetailEvent

    data class MentionSuggestionPicked(
        val candidateKey: String,
    ) : SessionDetailEvent

    data class MentionChipClicked(
        val ref: EntityRef,
    ) : SessionDetailEvent

    data class NpcStatusSelected(
        val npcId: Long,
        val status: NpcStatus,
    ) : SessionDetailEvent

    data class QuestStatusSelected(
        val questId: Long,
        val status: QuestStatus,
    ) : SessionDetailEvent

    data class RelatedNoteClicked(
        val sessionNoteId: Long,
    ) : SessionDetailEvent

    data object SheetDismissed : SessionDetailEvent

    data object SubmitEntryClicked : SessionDetailEvent

    data class EntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data class EditEntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object CancelEditEntryClicked : SessionDetailEvent

    data class DeleteEntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object EndSessionClicked : SessionDetailEvent

    data object ResumeSessionClicked : SessionDetailEvent
}
