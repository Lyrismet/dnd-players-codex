package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormEvent
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.combat.presentation.summary.CombatCardItem
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetEvent
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SessionDetailState(
    // a finished campaign is read-only, so the note composer stays hidden
    val readOnly: Boolean = false,
    val isLoading: Boolean = true,
    val header: SessionHeaderState = SessionHeaderState(),
    val feed: SessionFeedState = SessionFeedState(),
    // the fight whose delete is waiting for a yes
    val combatDeleteVisible: Boolean = false,
    val draft: String = "",
    val draftSelection: TextRange = TextRange.Zero,
    // the span the keyboard is still composing, kept so a suggestion underline survives the round trip
    val draftComposition: TextRange? = null,
    // the species just inserted that waits for its count to be typed, null otherwise
    val quantityHint: String? = null,
    val mentionSuggestions: List<MentionDropdownItem> = emptyList(),
    val selectedEntity: EntitySummaryItem? = null,
    // a card was tapped and its summary is still being built
    val entityLoading: Boolean = false,
    // the in-place editing of the open entity card
    val quickEdit: QuickEditState = QuickEditState(),
    val editingEntryTimeLabel: String? = null,
    val editingEntryId: Long? = null,
    val tagSheet: SessionTagSheetState? = null,
    // pinned and frequent phrases offered in the row above the composer
    val phraseStrip: List<SessionPhraseItem> = emptyList(),
    // phrases continuing the sentence being typed - empty while a mention is being typed or nothing matches
    val phraseCompletions: List<SessionPhraseItem> = emptyList(),
    val phraseSheet: PhraseSheetState? = null,
    val toast: String? = null,
    // the "?" tag sheet of a mention whose record is gone from the codex
    val deletedMentionSheet: DeletedMentionSheetState? = null,
    // the sheet that makes a record out of a typed name
    val mentionCreateSheet: MentionCreateSheetState? = null,
    // the codex form sheet, opened prefilled from the "?" tag sheet
    val entryForm: CodexEntryFormState? = null,
    val eventSink: (SessionDetailEvent) -> Unit = {},
) : CircuitUiState {
    val isEditingEntry: Boolean get() = editingEntryTimeLabel != null
}

/** what the header draws, apart from the state so a keystroke in the composer leaves it alone */
@Immutable
data class SessionHeaderState(
    val overline: String = "",
    val title: String = "",
    val dateLabel: String = "",
    val isLive: Boolean = true,
    val isTitleEditing: Boolean = false,
    val headerMentions: List<MentionChipItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagButtonLabel: String = "",
    val tagSuggestionCount: Int = 0,
)

/** what the note list draws, apart from the state so a keystroke in the composer leaves it alone */
@Immutable
data class SessionFeedState(
    val items: List<SessionFeedItem> = emptyList(),
    val readOnly: Boolean = false,
    val selectedEntryId: Long? = null,
    val editingEntryId: Long? = null,
    val highlightedEntryId: Long? = null,
    // feed position the list scrolls to while the highlight is active - null once the entry is not in the feed yet
    val focusIndex: Int? = null,
)

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

    /** a fight saved to the session, it sits among the notes by the time it ended */
    data class Combat(
        val card: CombatCardItem,
    ) : SessionFeedItem {
        override val key: String get() = "combat-${card.id}"
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
        // which token of its species this is in the note, tells apart two chips of the same species
        val occurrence: Int = 0,
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
    // the grey second line - race and place, who gave the quest, the path of a place
    val detail: String = "",
    // the nickname the query hit, shown ahead of the detail
    val nickname: String = "",
)

/** one line of the "@" dropdown */
sealed interface MentionDropdownItem {
    data class Header(
        val title: String,
    ) : MentionDropdownItem

    data class Entry(
        val suggestion: SessionMentionSuggestion,
    ) : MentionDropdownItem

    /** "+ Создать «X»…" - [name] is what was typed after the "@" */
    data class Create(
        val name: String,
    ) : MentionDropdownItem

    /** shown instead of the list while the codex has no records at all */
    data object EmptyCodex : MentionDropdownItem
}

/** the sheet that makes a record out of [name] - the record kind is picked from [kinds] */
data class MentionCreateSheetState(
    val name: String,
    val type: EntityKind,
    val kinds: List<EntityKind>,
    // how many notes carry the "?" tag this record is made for, null when the name comes from the draft
    val linkedNotes: Int?,
) {
    val canCreate: Boolean get() = name.isNotBlank()
}

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
        val composition: TextRange? = null,
    ) : SessionDetailEvent

    data object InsertMentionTriggerClicked : SessionDetailEvent

    data class MentionSuggestionPicked(
        val token: String,
    ) : SessionDetailEvent

    /** the "+ Создать «X»…" line of the dropdown */
    data class MentionCreateOpened(
        val name: String,
    ) : SessionDetailEvent

    data class MentionCreateNameChanged(
        val name: String,
    ) : SessionDetailEvent

    data class MentionCreateTypeChanged(
        val type: EntityKind,
    ) : SessionDetailEvent

    data object MentionCreateConfirmed : SessionDetailEvent

    data object MentionCreateDismissed : SessionDetailEvent

    /** a tap or change on the entity card that opens over this screen */
    data class EntitySheet(
        val event: EntitySheetEvent,
    ) : SessionDetailEvent

    /** an in-place edit on the entity card that opens over this screen */
    data class QuickEdit(
        val event: QuickEditUiEvent,
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

    data class CombatDeleteClicked(
        val id: Long,
    ) : SessionDetailEvent

    data object CombatDeleteConfirmed : SessionDetailEvent

    data object CombatDeleteDismissed : SessionDetailEvent

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

    /** a species chip was tapped, its card then shows and changes the count of that very chip */
    data class SpeciesChipClicked(
        val entryId: Long,
        val occurrence: Int,
        val speciesId: Long,
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
