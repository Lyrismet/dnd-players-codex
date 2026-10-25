package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.onEvent
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.mention.domain.dedupeKey
import com.lyrismet.incadent.feature.phrases.domain.phraseFragment
import com.lyrismet.incadent.feature.phrases.domain.repository.PhraseSnippetRepository
import com.lyrismet.incadent.feature.phrases.domain.snippetFromEntry
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetController
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_toast_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
internal class SessionDetailFields(
    val titleField: MutableState<String?>,
    val titleEditing: MutableState<Boolean>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
    val composer: SessionDraftController,
    val tags: SessionTagsController,
    val deleted: DeletedMentionController,
    val create: MentionCreateController,
    val phrases: PhraseSheetController,
    val speciesNote: SpeciesNoteController,
    val toast: MutableState<String?>,
    val formController: CodexEntryFormController,
    val quickEdit: QuickEditInteractions,
)

/** everything an event handler reads besides the editable fields - one value instead of five parameters */
internal data class EventContext(
    val currentNote: SessionNote?,
    val entries: List<SessionEntry>,
    val undoDeletedEntryTitle: String,
    val phraseSavedToast: String,
    val scope: CoroutineScope,
    val readOnly: Boolean,
    val combatToDelete: MutableState<Long?>,
)

/** turns every [SessionDetailEvent] into the matching controller call or repository write */
@Suppress("LongParameterList")
internal class SessionDetailEventHandler(
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val combatRepository: CombatRepository,
    private val phraseSnippetRepository: PhraseSnippetRepository,
    private val imageCompressor: ImageCompressor,
    private val mentionActions: SessionMentionActions,
) {
    // a card event that changes which card is open also forgets the chip it came from
    private fun onEntitySheet(
        event: EntitySheetEvent,
        entries: List<SessionEntry>,
        fields: SessionDetailFields,
        scope: CoroutineScope,
    ) {
        when (event) {
            is EntitySheetEvent.SpeciesNoteQuantityStepped -> fields.speciesNote.step(scope, entries, event.delta)
            is EntitySheetEvent.EntityClicked,
            EntitySheetEvent.Dismissed,
            -> {
                fields.speciesNote.clear()
                fields.quickEdit.onSheetClosed()
                fields.entitySheet.onEvent(scope, event)
            }
            else -> fields.entitySheet.onEvent(scope, event)
        }
    }

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun handle(
        event: SessionDetailEvent,
        context: EventContext,
        fields: SessionDetailFields,
        assignedTags: List<String>,
        tagSuggestions: List<SessionTagSuggestionItem>,
        mentionData: MentionEventData,
    ) {
        val currentNote = context.currentNote
        val entries = context.entries
        val undoDeletedEntryTitle = context.undoDeletedEntryTitle
        val phraseSavedToast = context.phraseSavedToast
        val scope = context.scope
        if (context.readOnly && event.writesCampaignData()) return
        when (event) {
            SessionDetailEvent.BackClicked -> navigator.pop()
            is SessionDetailEvent.TitleChanged -> onTitleChanged(event.title, currentNote, scope, fields.titleField)
            is SessionDetailEvent.DraftChanged ->
                fields.composer.onDraftChanged(
                    event.text,
                    event.selection,
                    event.composition,
                )
            SessionDetailEvent.InsertMentionTriggerClicked -> fields.composer.insertMentionTrigger()
            is SessionDetailEvent.MentionSuggestionPicked -> fields.composer.pickMention(event.token)
            is SessionDetailEvent.EntitySheet -> onEntitySheet(event.event, entries, fields, scope)
            is SessionDetailEvent.QuickEdit -> fields.quickEdit.onEvent(scope, event.event)
            is SessionDetailEvent.SpeciesChipClicked -> {
                fields.entitySheet.onEntityClicked(EntityRef.Species(event.speciesId))
                fields.speciesNote.open(SpeciesNotePoint(event.entryId, event.occurrence, event.speciesId))
            }
            SessionDetailEvent.SubmitEntryClicked -> fields.composer.submit(scope, entries, mentionData.candidates)
            is SessionDetailEvent.EntryClicked -> {
                fields.composer.toggleSelected(event.id)
                // the note's action row and the title editor never show together
                if (fields.composer.selectedEntryId.value != null) fields.titleEditing.value = false
            }
            SessionDetailEvent.TitleEditStarted -> {
                fields.titleEditing.value = true
                fields.composer.selectedEntryId.value = null
            }
            SessionDetailEvent.TitleEditFinished -> fields.titleEditing.value = false
            SessionDetailEvent.OutsideTapped -> {
                fields.composer.selectedEntryId.value = null
                fields.titleEditing.value = false
            }
            is SessionDetailEvent.EditEntryClicked -> fields.composer.edit(event.id, entries)
            SessionDetailEvent.CancelEditEntryClicked -> fields.composer.cancelEdit()
            is SessionDetailEvent.DeleteEntryClicked ->
                fields.composer.delete(scope, event.id, entries, undoDeletedEntryTitle)
            is SessionDetailEvent.CombatDeleteClicked -> context.combatToDelete.value = event.id
            SessionDetailEvent.CombatDeleteDismissed -> context.combatToDelete.value = null
            SessionDetailEvent.CombatDeleteConfirmed -> {
                val id = context.combatToDelete.value
                context.combatToDelete.value = null
                if (id != null) scope.launch { combatRepository.delete(id) }
            }
            SessionDetailEvent.EndSessionClicked -> onEndSession(currentNote, scope)
            SessionDetailEvent.ResumeSessionClicked -> onResumeSession(currentNote, scope)
            SessionDetailEvent.TagButtonClicked -> fields.tags.open()
            SessionDetailEvent.TagSheetDismissed -> fields.tags.dismiss()
            is SessionDetailEvent.TagToggled -> fields.tags.toggle(scope, event.name, assignedTags)
            is SessionDetailEvent.TagSuggestionAdded -> fields.tags.add(scope, event.tag)
            SessionDetailEvent.TagAddAllSuggestionsClicked -> fields.tags.addAll(scope, tagSuggestions.map { it.tag })
            is SessionDetailEvent.TagDraftChanged -> fields.tags.onDraftChanged(event.value)
            SessionDetailEvent.TagDraftSubmitted -> fields.tags.submitDraft(scope)
            is SessionDetailEvent.PhraseStripPicked -> fields.composer.insertPhrase(event.text, replacing = null)
            is SessionDetailEvent.PhraseCompletionPicked ->
                fields.composer.insertPhrase(
                    event.text,
                    replacing = phraseFragment(fields.composer.draft.value.text),
                )
            SessionDetailEvent.PhraseSheetOpened -> fields.phrases.open()
            is SessionDetailEvent.PhraseSheet -> fields.phrases.onEvent(event.event, scope)
            is SessionDetailEvent.SaveEntryAsPhraseClicked ->
                onSaveEntryAsPhrase(event.id, entries, phraseSavedToast, scope, fields)
            is SessionDetailEvent.DeletedMentionClicked -> fields.deleted.show(event.deleted)
            SessionDetailEvent.DeletedMentionSheetDismissed -> fields.deleted.dismiss()
            is SessionDetailEvent.DeletedMentionPickingChanged -> fields.deleted.setPicking(event.isPicking)
            is SessionDetailEvent.DeletedMentionQueryChanged -> fields.deleted.setQuery(event.query)
            SessionDetailEvent.DeletedMentionCreateClicked -> fields.deleted.createRecord(fields.create::startFor)
            is SessionDetailEvent.MentionCreateOpened -> fields.create.start(event.name)
            is SessionDetailEvent.MentionCreateNameChanged -> fields.create.setName(event.name)
            is SessionDetailEvent.MentionCreateTypeChanged -> fields.create.setType(event.type)
            SessionDetailEvent.MentionCreateConfirmed -> fields.create.confirm(scope, mentionData)
            SessionDetailEvent.MentionCreateDismissed -> fields.create.dismiss()
            is SessionDetailEvent.DeletedMentionLinked -> fields.deleted.rewrite(scope, event.key, mentionData)
            SessionDetailEvent.DeletedMentionKeptAsText -> fields.deleted.rewrite(scope, null, mentionData)
            is SessionDetailEvent.AutoMentionsLinkClicked ->
                entries.find { it.id == event.entryId }?.let {
                    mentionActions.linkAutoMatches(scope, it, mentionData, only = null)
                }
            is SessionDetailEvent.AutoMentionsDismissed -> mentionActions.dismissAutoMatches(scope, event.entryId)
            is SessionDetailEvent.AutoMentionWordClicked ->
                entries.find { it.id == event.entryId }?.let { entry ->
                    mentionData.candidates.entities
                        .firstOrNull { it.dedupeKey() == event.entityKey }
                        ?.let { mentionActions.linkAutoMatches(scope, entry, mentionData, only = it) }
                }
            is SessionDetailEvent.EntryForm -> {
                val closed =
                    fields.formController.onEvent(event.event, scope, imageCompressor) { name ->
                        fields.toast.value = getString(Res.string.codex_entry_quick_add_toast_format, name)
                    }
                if (closed) fields.toast.value = null
            }
        }
    }

    private fun onSaveEntryAsPhrase(
        id: Long,
        entries: List<SessionEntry>,
        savedToast: String,
        scope: CoroutineScope,
        fields: SessionDetailFields,
    ) {
        val entry = entries.find { it.id == id } ?: return
        fields.composer.selectedEntryId.value = null
        scope.launch { phraseSnippetRepository.add(snippetFromEntry(entry.body)) }
        fields.toast.value = savedToast
    }

    private fun onTitleChanged(
        title: String,
        currentNote: SessionNote?,
        scope: CoroutineScope,
        titleField: MutableState<String?>,
    ) {
        titleField.value = title
        currentNote?.let { note -> scope.launch { sessionNoteRepository.upsert(note.copy(title = title)) } }
    }

    private fun onEndSession(
        currentNote: SessionNote?,
        scope: CoroutineScope,
    ) {
        currentNote?.let { note ->
            scope.launch {
                val endedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                sessionNoteRepository.upsert(note.copy(endedAt = endedAt))
            }
        }
    }

    private fun onResumeSession(
        currentNote: SessionNote?,
        scope: CoroutineScope,
    ) {
        currentNote?.let { note ->
            scope.launch {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                sessionNoteRepository.makeOnlyLive(note.id, now)
            }
        }
    }
}
