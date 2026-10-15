package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.entitysummary.EntitySheetEvent
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.phrases.PhraseSheetEvent
import com.lyrismet.incadent.core.phrases.PhraseSheetState
import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.presentation.codex.CodexEntryFormEvent
import com.lyrismet.incadent.presentation.codex.CodexEntryFormState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** what an entity sheet calls to open a session at one of its notes */
internal fun Navigator.sessionEntryOpener(): (Long, Long) -> Unit =
    { sessionNoteId, entryId -> goTo(SessionDetailScreen(sessionNoteId, focusEntryId = entryId)) }

@Serializable
data class SessionDetailScreen(
    val sessionNoteId: Long,
    // set when opened from a related note - the entry is scrolled to and highlighted
    val focusEntryId: Long? = null,
) : Screen

data class SessionDetailState(
    // a finished campaign is read-only, so the note composer stays hidden
    val readOnly: Boolean = false,
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
    val isTitleEditing: Boolean = false,
    val editingEntryId: Long? = null,
    val highlightedEntryId: Long? = null,
    // feed position the list scrolls to while the highlight is active - null once the entry is not in the feed yet
    val focusIndex: Int? = null,
    val tags: List<String> = emptyList(),
    val tagButtonLabel: String = "",
    val tagSuggestionCount: Int = 0,
    val tagSheet: SessionTagSheetState? = null,
    // pinned and frequent phrases offered in the row above the composer
    val phraseStrip: List<SessionPhraseItem> = emptyList(),
    // phrases continuing the sentence being typed - empty while a mention is being typed or nothing matches
    val phraseCompletions: List<SessionPhraseItem> = emptyList(),
    val phraseSheet: PhraseSheetState? = null,
    val toast: String? = null,
    // the "?" tag sheet of a mention whose record is gone from the codex
    val deletedMentionSheet: DeletedMentionSheetState? = null,
    // the codex form sheet, opened prefilled from the "?" tag sheet
    val entryForm: CodexEntryFormState? = null,
    val eventSink: (SessionDetailEvent) -> Unit = {},
) : CircuitUiState {
    val isEditingEntry: Boolean get() = editingEntryTimeLabel != null
}

/** the "Теги сессии" bottom sheet's content - absent (null on [SessionDetailState]) while the sheet is closed */
data class SessionTagSheetState(
    val overline: String,
    val sessionTitle: String,
    val suggestions: List<SessionTagSuggestionItem>,
    val catalog: List<SessionTagCatalogItem>,
    val draft: String,
)

data class SessionTagSuggestionItem(
    val tag: String,
    val reason: String,
)

data class SessionTagCatalogItem(
    val name: String,
    val assigned: Boolean,
)

/** a phrase chip or completion row - [entryCount] is the number of notes a frequent phrase was found in, 0 if pinned */
data class SessionPhraseItem(
    val text: String,
    val label: String,
    val isPinned: Boolean,
    val entryCount: Int,
)

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
    val edited: Boolean,
    val segments: List<SessionEntrySegment>,
    // how many codex records the note names without "@" and has not decided on yet - the row under the note shows it
    val autoMatchCount: Int = 0,
)

sealed interface SessionEntrySegment {
    data class Text(
        val text: String,
    ) : SessionEntrySegment

    data class Mention(
        val chip: MentionChipItem,
    ) : SessionEntrySegment

    /** a codex name written without "@" - drawn with a dotted underline and linked or left alone on request */
    data class AutoMatch(
        val text: String,
        val entityKey: String,
    ) : SessionEntrySegment
}

data class SessionMentionSuggestion(
    val token: String,
    val glyph: MentionGlyph,
    val tint: Color,
    val name: String,
    val typeLabel: String,
)

sealed interface SessionDetailEvent : CircuitUiEvent {
    data object BackClicked : SessionDetailEvent

    data object TitleEditStarted : SessionDetailEvent

    data object TitleEditFinished : SessionDetailEvent

    data class TitleChanged(
        val title: String,
    ) : SessionDetailEvent

    data class DraftChanged(
        val text: String,
        val selection: TextRange,
    ) : SessionDetailEvent

    data object InsertMentionTriggerClicked : SessionDetailEvent

    data class MentionSuggestionPicked(
        val token: String,
    ) : SessionDetailEvent

    /** a tap or change on the entity card that opens over this screen */
    data class EntitySheet(
        val event: EntitySheetEvent,
    ) : SessionDetailEvent

    data object SubmitEntryClicked : SessionDetailEvent

    data class EntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data class EditEntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object CancelEditEntryClicked : SessionDetailEvent

    data object OutsideTapped : SessionDetailEvent

    data class DeleteEntryClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object EndSessionClicked : SessionDetailEvent

    data object ResumeSessionClicked : SessionDetailEvent

    data object TagButtonClicked : SessionDetailEvent

    data object TagSheetDismissed : SessionDetailEvent

    data class TagToggled(
        val name: String,
    ) : SessionDetailEvent

    data class TagSuggestionAdded(
        val tag: String,
    ) : SessionDetailEvent

    data object TagAddAllSuggestionsClicked : SessionDetailEvent

    data class TagDraftChanged(
        val value: String,
    ) : SessionDetailEvent

    data object TagDraftSubmitted : SessionDetailEvent

    data class PhraseStripPicked(
        val text: String,
    ) : SessionDetailEvent

    data class PhraseCompletionPicked(
        val text: String,
    ) : SessionDetailEvent

    data object PhraseSheetOpened : SessionDetailEvent

    data class PhraseSheet(
        val event: PhraseSheetEvent,
    ) : SessionDetailEvent

    data class SaveEntryAsPhraseClicked(
        val id: Long,
    ) : SessionDetailEvent

    /** a "?" tag was tapped */
    data class DeletedMentionClicked(
        val deleted: DeletedMention,
    ) : SessionDetailEvent

    data object DeletedMentionSheetDismissed : SessionDetailEvent

    data object DeletedMentionCreateClicked : SessionDetailEvent

    /** opens or closes the list of records the tag can be linked to */
    data class DeletedMentionPickingChanged(
        val isPicking: Boolean,
    ) : SessionDetailEvent

    data class DeletedMentionQueryChanged(
        val query: String,
    ) : SessionDetailEvent

    /** [key] is the option id of the picked record */
    data class DeletedMentionLinked(
        val key: String,
    ) : SessionDetailEvent

    data object DeletedMentionKeptAsText : SessionDetailEvent

    /** "Связать" under a note - every auto matched word of it gets its "@" */
    data class AutoMentionsLinkClicked(
        val entryId: Long,
    ) : SessionDetailEvent

    data class AutoMentionsDismissed(
        val entryId: Long,
    ) : SessionDetailEvent

    /** one dotted word was tapped - only that record is linked in the note */
    data class AutoMentionWordClicked(
        val entryId: Long,
        val entityKey: String,
    ) : SessionDetailEvent

    data class EntryForm(
        val event: CodexEntryFormEvent,
    ) : SessionDetailEvent
}
