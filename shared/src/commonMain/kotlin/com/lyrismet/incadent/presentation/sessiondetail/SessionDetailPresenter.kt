package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.designsystem.LocationMentionColor
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.designsystem.component.toChipItem
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.format.sessionNumberLabels
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.format.toShortDayMonthUpper
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.MentionSegment
import com.lyrismet.incadent.core.mention.insertMention
import com.lyrismet.incadent.core.mention.matchingMentionEntities
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.mention.mentionKey
import com.lyrismet.incadent.core.mention.mentionsIn
import com.lyrismet.incadent.core.mention.parseMentions
import com.lyrismet.incadent.core.mention.trailingMentionQuery
import com.lyrismet.incadent.core.phrases.PhraseOption
import com.lyrismet.incadent.core.phrases.PhraseSheetController
import com.lyrismet.incadent.core.phrases.insertPhrase
import com.lyrismet.incadent.core.phrases.phraseCompletions
import com.lyrismet.incadent.core.phrases.phraseFragment
import com.lyrismet.incadent.core.phrases.phraseLabel
import com.lyrismet.incadent.core.phrases.phrasePool
import com.lyrismet.incadent.core.phrases.rememberFrequentPhrases
import com.lyrismet.incadent.core.phrases.sheetState
import com.lyrismet.incadent.core.phrases.snippetFromEntry
import com.lyrismet.incadent.core.tags.TagReasonLabels
import com.lyrismet.incadent.core.tags.normalizeCustomTagName
import com.lyrismet.incadent.core.tags.suggestSessionTags
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.PhraseSnippetRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_meeting_separator_format
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import dndplayerscodex.shared.generated.resources.session_detail_phrase_saved_toast
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import dndplayerscodex.shared.generated.resources.session_detail_tag_button_label_empty
import dndplayerscodex.shared.generated.resources.session_detail_tag_button_label_has_tags
import dndplayerscodex.shared.generated.resources.session_detail_tag_sheet_overline_format
import dndplayerscodex.shared.generated.resources.session_detail_undo_deleted_entry_title
import dndplayerscodex.shared.generated.resources.session_overline_format
import dndplayerscodex.shared.generated.resources.session_tag_reason_quest
import dndplayerscodex.shared.generated.resources.session_tag_reason_quests
import dndplayerscodex.shared.generated.resources.session_tag_reason_words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val ENTRY_PREVIEW_LENGTH = 60
private const val HIGHLIGHT_DURATION_MS = 2800L
private const val PHRASE_STRIP_SIZE = 8

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
private class SessionDetailFields(
    val titleField: MutableState<String?>,
    val draft: MutableState<TextFieldValue>,
    val editingEntryId: MutableState<Long?>,
    val selectedEntryId: MutableState<Long?>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
    val tagSheetOpen: MutableState<Boolean>,
    val tagDraft: MutableState<String>,
    val phrases: PhraseSheetController,
    val toast: MutableState<String?>,
)

@Suppress("LongParameterList")
class SessionDetailPresenter(
    private val screen: SessionDetailScreen,
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val partyRepository: PartyRepository,
    private val mentionRepositories: MentionRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val phraseSnippetRepository: PhraseSnippetRepository,
) : Presenter<SessionDetailState> {
    // one collection per source the screen reads - a flat merge, not real complexity
    @Suppress("LongMethod")
    @OptIn(ExperimentalTime::class)
    @Composable
    override fun present(): SessionDetailState {
        val allSessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val entries by sessionEntryRepository
            .observeForSession(screen.sessionNoteId)
            .collectAsState(initial = emptyList())
        val npcs by mentionRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by mentionRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by mentionRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val parties by partyRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val pinnedPhrases by phraseSnippetRepository.observeAll().collectAsState(initial = emptyList())
        val frequentPhrases = rememberFrequentPhrases(allEntries)
        val scope = rememberCoroutineScope()

        val currentNote = allSessions.find { it.id == screen.sessionNoteId }
        val numberLabel = allSessions.sessionNumberLabels(numbering)[screen.sessionNoteId]

        val fields = rememberFields()
        val highlightedEntryId = remember { mutableStateOf(screen.focusEntryId) }
        LaunchedEffect(highlightedEntryId.value) {
            if (highlightedEntryId.value != null) {
                delay(HIGHLIGHT_DURATION_MS)
                highlightedEntryId.value = null
            }
        }
        fields.toast.autoDismiss()
        val titleField = fields.titleField
        val draft = fields.draft
        val editingEntryId = fields.editingEntryId
        val selectedEntryId = fields.selectedEntryId
        val selectedEntityRef = fields.selectedEntityRef

        LaunchedEffect(currentNote?.id) {
            if (titleField.value == null && currentNote != null) {
                titleField.value = currentNote.title
            }
        }

        val mention = buildMentionContext(npcs, locations, quests, draft.value.text)
        val feed = buildFeed(entries, currentNote?.sessionDate?.date, mention.candidates)
        val focusIndex =
            highlightedEntryId.value?.let { id ->
                feed.indexOfFirst { it is SessionFeedItem.Note && it.entry.id == id }.takeIf { it >= 0 }
            }
        val entryMentions = mentionsIn(entries.map { it.body }, mention.candidates)
        val headerMentions = entryMentions.map { it.toChipItem() }

        val selectedEntity =
            selectedEntitySummary(
                selectedEntityRef.value,
                npcs,
                locations,
                quests,
                allSessions,
                allEntries,
                mention.candidates,
                numbering,
                parties,
            )

        val editingEntryTimeLabel =
            editingEntryId.value?.let { id -> entries.find { it.id == id }?.createdAt?.toDisplayTime() }
        val undoDeletedEntryTitle = stringResource(Res.string.session_detail_undo_deleted_entry_title)
        val titleText = titleField.value ?: currentNote?.title.orEmpty()

        val assignedTags by tagRepository.observeTagsFor(screen.sessionNoteId).collectAsState(initial = emptyList())
        val tagCatalog by tagRepository.observeCatalog().collectAsState(initial = emptyList())
        val mentionedQuestNames = entryMentions.filterIsInstance<MentionEntity.QuestMention>().map { it.name }
        val tagSuggestions =
            suggestSessionTags(entries.map { it.body }, mentionedQuestNames, assignedTags, tagReasonLabels())
                .map { SessionTagSuggestionItem(it.tag, it.reason) }
        val tagButtonLabel =
            if (assignedTags.isEmpty()) {
                stringResource(Res.string.session_detail_tag_button_label_empty)
            } else {
                stringResource(Res.string.session_detail_tag_button_label_has_tags)
            }
        val tagSheet =
            if (fields.tagSheetOpen.value) {
                SessionTagSheetState(
                    overline =
                        numberLabel
                            ?.let { stringResource(Res.string.session_detail_tag_sheet_overline_format, it) }
                            .orEmpty(),
                    sessionTitle = titleText,
                    suggestions = tagSuggestions,
                    catalog =
                        tagCatalog.map { name ->
                            SessionTagCatalogItem(name, assignedTags.any { it.equals(name, ignoreCase = true) })
                        },
                    draft = fields.tagDraft.value,
                )
            } else {
                null
            }

        val phraseSavedToast = stringResource(Res.string.session_detail_phrase_saved_toast)
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val phrasePool = remember(pinnedPhrases, frequentPhrases) { phrasePool(pinnedPhrases, frequentPhrases) }
        val completions =
            if (mention.suggestions.isEmpty()) phraseCompletions(draft.value.text, phrasePool)?.options else null

        // deliberately renders the same header/feed/composer shape whether or not currentNote
        // has arrived yet from the Flow's cold start - swapping to a distinct "loading" screen
        // for that one frame was the cause of a visible flash on every navigation into this screen
        return SessionDetailState(
            isLoading = currentNote == null,
            overline = numberLabel?.let { stringResource(Res.string.session_overline_format, it) }.orEmpty(),
            title = titleText,
            dateLabel = currentNote?.sessionDate?.toDisplayDate().orEmpty(),
            isLive = currentNote?.isLive ?: true,
            headerMentions = headerMentions,
            feed = feed,
            draft = draft.value.text,
            draftSelection = draft.value.selection,
            mentionSuggestions = mention.suggestions,
            selectedEntity = selectedEntity,
            editingEntryTimeLabel = editingEntryTimeLabel,
            selectedEntryId = selectedEntryId.value,
            editingEntryId = editingEntryId.value,
            highlightedEntryId = highlightedEntryId.value,
            focusIndex = focusIndex,
            tags = assignedTags,
            tagButtonLabel = tagButtonLabel,
            tagSuggestionCount = tagSuggestions.size,
            tagSheet = tagSheet,
            phraseStrip = phrasePool.take(PHRASE_STRIP_SIZE).map { it.toItem(questPrefix) },
            phraseCompletions = completions.orEmpty().map { it.toItem(questPrefix) },
            phraseSheet = fields.phrases.sheetState(pinnedPhrases, frequentPhrases),
            toast = fields.toast.value,
        ) { event ->
            val context = EventContext(currentNote, entries, undoDeletedEntryTitle, phraseSavedToast, scope)
            onEvent(event, context, fields, assignedTags, tagSuggestions)
        }
    }

    // edited locally so live Flow re-emissions from other screens don't clobber in-progress typing
    @Composable
    private fun rememberFields(): SessionDetailFields {
        val noteId = screen.sessionNoteId
        val selectedEntityRef = remember(noteId) { mutableStateOf<EntityRef?>(null) }
        return SessionDetailFields(
            titleField = remember(noteId) { mutableStateOf<String?>(null) },
            draft = remember(noteId) { mutableStateOf(TextFieldValue("")) },
            editingEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
            selectedEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
            selectedEntityRef = selectedEntityRef,
            entitySheet = entitySheetInteractions(selectedEntityRef),
            tagSheetOpen = remember(noteId) { mutableStateOf(false) },
            tagDraft = remember(noteId) { mutableStateOf("") },
            phrases = remember { PhraseSheetController(phraseSnippetRepository) },
            toast = remember(noteId) { mutableStateOf<String?>(null) },
        )
    }

    private fun entitySheetInteractions(selectedEntityRef: MutableState<EntityRef?>) =
        EntitySheetInteractions(
            selectedEntityRef,
            mentionRepositories,
            partyRepository,
            navigator,
        )

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun onEvent(
        event: SessionDetailEvent,
        context: EventContext,
        fields: SessionDetailFields,
        assignedTags: List<String>,
        tagSuggestions: List<SessionTagSuggestionItem>,
    ) {
        val currentNote = context.currentNote
        val entries = context.entries
        val undoDeletedEntryTitle = context.undoDeletedEntryTitle
        val phraseSavedToast = context.phraseSavedToast
        val scope = context.scope
        when (event) {
            SessionDetailEvent.BackClicked -> navigator.pop()
            is SessionDetailEvent.TitleChanged -> onTitleChanged(event.title, currentNote, scope, fields.titleField)
            is SessionDetailEvent.DraftChanged ->
                fields.draft.value = TextFieldValue(event.text, event.selection)
            SessionDetailEvent.InsertMentionTriggerClicked -> onInsertMentionTrigger(fields.draft)
            is SessionDetailEvent.MentionSuggestionPicked ->
                onMentionSuggestionPicked(event.candidateKey, fields.draft)
            is SessionDetailEvent.MentionChipClicked -> fields.entitySheet.onEntityClicked(event.ref)
            is SessionDetailEvent.NpcStatusSelected ->
                fields.entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is SessionDetailEvent.NpcLifeSelected ->
                fields.entitySheet.onNpcLifeSelected(scope, event.npcId, event.lifeState)
            is SessionDetailEvent.QuestStatusSelected ->
                fields.entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is SessionDetailEvent.RelatedNoteClicked ->
                fields.entitySheet.onRelatedNoteClicked(event.sessionNoteId, event.entryId)
            SessionDetailEvent.SheetDismissed -> fields.entitySheet.onDismissed()
            SessionDetailEvent.SubmitEntryClicked -> onSubmitEntry(scope, entries, fields)
            is SessionDetailEvent.EntryClicked -> onEntryClicked(event.id, fields.selectedEntryId)
            is SessionDetailEvent.EditEntryClicked -> onEditEntry(event.id, entries, fields)
            SessionDetailEvent.CancelEditEntryClicked -> {
                fields.editingEntryId.value = null
                fields.draft.value = TextFieldValue("")
            }
            is SessionDetailEvent.DeleteEntryClicked ->
                onDeleteEntryClicked(
                    event.id,
                    entries,
                    undoDeletedEntryTitle,
                    scope,
                    fields,
                    sessionEntryRepository,
                    undoController,
                )
            SessionDetailEvent.EndSessionClicked -> onEndSession(currentNote, scope)
            SessionDetailEvent.ResumeSessionClicked -> onResumeSession(currentNote, scope)
            SessionDetailEvent.TagButtonClicked -> fields.tagSheetOpen.value = true
            SessionDetailEvent.TagSheetDismissed -> fields.tagSheetOpen.value = false
            is SessionDetailEvent.TagToggled -> onTagToggled(event.name, assignedTags, scope)
            is SessionDetailEvent.TagSuggestionAdded ->
                scope.launch { tagRepository.addTag(screen.sessionNoteId, event.tag) }
            SessionDetailEvent.TagAddAllSuggestionsClicked -> onTagAddAllSuggestions(tagSuggestions, scope)
            is SessionDetailEvent.TagDraftChanged -> fields.tagDraft.value = event.value
            SessionDetailEvent.TagDraftSubmitted -> onTagDraftSubmitted(fields.tagDraft, scope)
            is SessionDetailEvent.PhraseStripPicked -> insertPhraseIntoDraft(fields.draft, event.text, replacing = null)
            is SessionDetailEvent.PhraseCompletionPicked ->
                insertPhraseIntoDraft(fields.draft, event.text, replacing = phraseFragment(fields.draft.value.text))
            SessionDetailEvent.PhraseSheetOpened -> fields.phrases.open()
            is SessionDetailEvent.PhraseSheet -> fields.phrases.onEvent(event.event, scope)
            is SessionDetailEvent.SaveEntryAsPhraseClicked ->
                onSaveEntryAsPhrase(event.id, entries, phraseSavedToast, scope, fields)
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
        fields.selectedEntryId.value = null
        scope.launch { phraseSnippetRepository.add(snippetFromEntry(entry.body)) }
        fields.toast.value = savedToast
    }

    private fun onTagToggled(
        name: String,
        assignedTags: List<String>,
        scope: CoroutineScope,
    ) {
        val isAssigned = assignedTags.any { it.equals(name, ignoreCase = true) }
        scope.launch {
            if (isAssigned) {
                tagRepository.removeTag(screen.sessionNoteId, name)
            } else {
                tagRepository.addTag(screen.sessionNoteId, name)
            }
        }
    }

    private fun onTagAddAllSuggestions(
        tagSuggestions: List<SessionTagSuggestionItem>,
        scope: CoroutineScope,
    ) {
        scope.launch { tagSuggestions.forEach { tagRepository.addTag(screen.sessionNoteId, it.tag) } }
    }

    private fun onTagDraftSubmitted(
        tagDraft: MutableState<String>,
        scope: CoroutineScope,
    ) {
        val name = normalizeCustomTagName(tagDraft.value) ?: return
        tagDraft.value = ""
        scope.launch { tagRepository.addTag(screen.sessionNoteId, name) }
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

    // cursor always lands at the end of the new text, not stuck at the caret's old position
    private fun onInsertMentionTrigger(draft: MutableState<TextFieldValue>) {
        val current = draft.value.text
        val next = if (current.isEmpty() || current.endsWith(" ")) "$current@" else "$current @"
        draft.value = TextFieldValue(next, TextRange(next.length))
    }

    private fun onMentionSuggestionPicked(
        candidateKey: String,
        draft: MutableState<TextFieldValue>,
    ) {
        val next = insertMention(draft.value.text, candidateKey)
        draft.value = TextFieldValue(next, TextRange(next.length))
    }

    private fun onSubmitEntry(
        scope: CoroutineScope,
        entries: List<SessionEntry>,
        fields: SessionDetailFields,
    ) {
        val body =
            fields.draft.value.text
                .trim()
        if (body.isEmpty()) return
        val editingId = fields.editingEntryId.value
        fields.draft.value = TextFieldValue("")
        fields.editingEntryId.value = null
        // an unchanged save must not flip the "изм." mark, so it only writes when the body actually differs
        if (editingId != null && entries.find { it.id == editingId }?.body == body) return
        scope.launch {
            if (editingId != null) {
                sessionEntryRepository.update(editingId, body)
            } else {
                sessionEntryRepository.add(screen.sessionNoteId, body)
            }
        }
    }

    private fun onEditEntry(
        id: Long,
        entries: List<SessionEntry>,
        fields: SessionDetailFields,
    ) {
        val entry = entries.find { it.id == id } ?: return
        fields.selectedEntryId.value = null
        fields.editingEntryId.value = id
        fields.draft.value = TextFieldValue(entry.body, TextRange(entry.body.length))
    }

    @OptIn(ExperimentalTime::class)
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

    @OptIn(ExperimentalTime::class)
    private fun onResumeSession(
        currentNote: SessionNote?,
        scope: CoroutineScope,
    ) {
        currentNote?.let { note ->
            scope.launch {
                sessionNoteRepository.upsert(note.copy(endedAt = null))
                // only one session may be live at a time - resuming this one closes out any other
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                sessionNoteRepository.endOtherLiveSessions(exceptId = note.id, endedAt = now)
            }
        }
    }
}

// tapping the already selected note deselects it
private fun onEntryClicked(
    id: Long,
    selectedEntryId: MutableState<Long?>,
) {
    selectedEntryId.value = if (selectedEntryId.value == id) null else id
}

/** everything an event handler reads besides the editable fields - one value instead of five parameters */
private data class EventContext(
    val currentNote: SessionNote?,
    val entries: List<SessionEntry>,
    val undoDeletedEntryTitle: String,
    val phraseSavedToast: String,
    val scope: CoroutineScope,
)

// the cursor lands after the inserted phrase, same as a picked mention
private fun insertPhraseIntoDraft(
    draft: MutableState<TextFieldValue>,
    phrase: String,
    replacing: String?,
) {
    val next = insertPhrase(draft.value.text, phrase, replacing)
    draft.value = TextFieldValue(next, TextRange(next.length))
}

private fun PhraseOption.toItem(questPrefix: String) =
    SessionPhraseItem(
        text = text,
        label = phraseLabel(text, questPrefix),
        isPinned = isPinned,
        entryCount = entryCount,
    )

private data class MentionContext(
    val candidates: List<MentionCandidate>,
    val suggestions: List<SessionMentionSuggestion>,
)

// deletes immediately and offers undo - clears in-progress editing if the edited entry is the one removed
@Suppress("LongParameterList")
private fun onDeleteEntryClicked(
    id: Long,
    entries: List<SessionEntry>,
    undoDeletedEntryTitle: String,
    scope: CoroutineScope,
    fields: SessionDetailFields,
    sessionEntryRepository: SessionEntryRepository,
    undoController: UndoController,
) {
    val entry = entries.find { it.id == id } ?: return
    fields.selectedEntryId.value = null
    if (fields.editingEntryId.value == id) {
        fields.editingEntryId.value = null
        fields.draft.value = TextFieldValue("")
    }
    scope.launch {
        sessionEntryRepository.delete(id)
        val preview = entry.body.take(ENTRY_PREVIEW_LENGTH)
        val suffix = if (entry.body.length > ENTRY_PREVIEW_LENGTH) "…" else ""
        undoController.show(undoDeletedEntryTitle, "${entry.createdAt.toDisplayTime()} · $preview$suffix") {
            sessionEntryRepository.restore(entry)
        }
    }
}

/** resolves mention candidates once and, from the same data, the trailing-@-query autocomplete suggestions */
@Composable
private fun buildMentionContext(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    draftText: String,
): MentionContext {
    val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
    val mentionEntities = mentionEntitiesFrom(npcs, locations, quests)
    val candidates = mentionCandidates(mentionEntities, questPrefix)

    val npcTypeLabel = stringResource(Res.string.session_detail_mention_type_npc)
    val locationTypeLabel = stringResource(Res.string.session_detail_mention_type_location)
    val questTypeLabel = stringResource(Res.string.session_detail_mention_type_quest)
    val suggestions =
        trailingMentionQuery(draftText)
            ?.let { matchingMentionEntities(mentionEntities, it, questPrefix) }
            .orEmpty()
            .map { it.toSuggestion(questPrefix, npcTypeLabel, locationTypeLabel, questTypeLabel) }

    return MentionContext(candidates, suggestions)
}

/** groups [entries] under a "ВСТРЕЧА N" separator per calendar day, only when the session spans more than one */
@Composable
private fun buildFeed(
    entries: List<SessionEntry>,
    sessionStartDay: LocalDate?,
    candidates: List<MentionCandidate>,
): List<SessionFeedItem> {
    if (entries.isEmpty()) return emptyList()
    val distinctDays = (listOfNotNull(sessionStartDay) + entries.map { it.createdAt.date }).distinct().sorted()
    val isMultiDay = distinctDays.size > 1

    val feed = mutableListOf<SessionFeedItem>()
    var lastDay: LocalDate? = null
    for (entry in entries) {
        val day = entry.createdAt.date
        if (isMultiDay && day != lastDay) {
            val meetingNumber = distinctDays.indexOf(day) + 1
            val label =
                stringResource(
                    Res.string.session_detail_meeting_separator_format,
                    meetingNumber,
                    day.toShortDayMonthUpper(),
                )
            feed += SessionFeedItem.DaySeparator(key = "sep-$day", label = label)
            lastDay = day
        }
        feed += SessionFeedItem.Note(entry.toItem(candidates))
    }
    return feed
}

private fun SessionEntry.toItem(candidates: List<MentionCandidate>): SessionEntryItem =
    SessionEntryItem(
        id = id,
        timeLabel = createdAt.toDisplayTime(),
        edited = edited,
        segments = parseMentions(body, candidates).map { it.toSegment() },
    )

private fun MentionSegment.toSegment(): SessionEntrySegment =
    when (this) {
        is MentionSegment.Text -> SessionEntrySegment.Text(text)
        is MentionSegment.Mention -> SessionEntrySegment.Mention(entity.toChipItem())
    }

private fun MentionEntity.toSuggestion(
    questPrefix: String,
    npcTypeLabel: String,
    locationTypeLabel: String,
    questTypeLabel: String,
): SessionMentionSuggestion {
    val (glyph, tint, typeLabel) =
        when (this) {
            is MentionEntity.NpcMention -> Triple(MentionGlyph.NPC, status.toStatusColor().foreground, npcTypeLabel)
            is MentionEntity.LocationMention ->
                Triple(MentionGlyph.LOCATION, LocationMentionColor.foreground, locationTypeLabel)
            is MentionEntity.QuestMention ->
                Triple(MentionGlyph.QUEST, status.toStatusColor().foreground, questTypeLabel)
        }
    return SessionMentionSuggestion(
        candidateKey = mentionKey(this, questPrefix),
        glyph = glyph,
        tint = tint,
        name = name,
        typeLabel = typeLabel,
    )
}

@Composable
private fun tagReasonLabels() =
    TagReasonLabels(
        quest = stringResource(Res.string.session_tag_reason_quest),
        quests = stringResource(Res.string.session_tag_reason_quests),
        words = stringResource(Res.string.session_tag_reason_words),
    )
