package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.errors.ReportFailures
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.form.rememberPlaceFailureText
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.onEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.rememberFullPhoto
import com.lyrismet.incadent.feature.codex.presentation.summary.selectedEntitySummary
import com.lyrismet.incadent.feature.codex.presentation.summary.withFullPhoto
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.dedupeKey
import com.lyrismet.incadent.feature.mention.domain.mentionsIn
import com.lyrismet.incadent.feature.mention.domain.pendingQuantityName
import com.lyrismet.incadent.feature.mention.domain.plainMentionText
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.phrases.domain.PhraseOption
import com.lyrismet.incadent.feature.phrases.domain.phraseCompletions
import com.lyrismet.incadent.feature.phrases.domain.phraseFragment
import com.lyrismet.incadent.feature.phrases.domain.phraseLabel
import com.lyrismet.incadent.feature.phrases.domain.phrasePool
import com.lyrismet.incadent.feature.phrases.domain.repository.PhraseSnippetRepository
import com.lyrismet.incadent.feature.phrases.domain.snippetFromEntry
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetController
import com.lyrismet.incadent.feature.phrases.presentation.rememberFrequentPhrases
import com.lyrismet.incadent.feature.phrases.presentation.sheetState
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import com.lyrismet.incadent.feature.session.domain.tags.TagReasonLabels
import com.lyrismet.incadent.feature.session.domain.tags.suggestSessionTags
import com.lyrismet.incadent.feature.session.presentation.route.SessionDetailScreen
import com.lyrismet.incadent.feature.session.presentation.route.sessionEntryOpener
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_toast_format
import dndplayerscodex.shared.generated.resources.session_detail_phrase_saved_toast
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private val HIGHLIGHT_DURATION = 2800.milliseconds
private const val PHRASE_STRIP_SIZE = 8

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
private class SessionDetailFields(
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
)

@Suppress("LongParameterList")
class SessionDetailPresenter(
    private val screen: SessionDetailScreen,
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val codexRepositories: CodexRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val phraseSnippetRepository: PhraseSnippetRepository,
    private val imageCompressor: ImageCompressor,
    private val campaignRepository: CampaignRepository,
) : Presenter<SessionDetailState> {
    private val mentionActions = SessionMentionActions(sessionEntryRepository, undoController)

    // one collection per source the screen reads - a flat merge, not real complexity
    @Suppress("LongMethod")
    @Composable
    override fun present(): SessionDetailState {
        val allSessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val entries by sessionEntryRepository
            .observeForSession(screen.sessionNoteId)
            .collectAsState(initial = emptyList())
        val npcs by codexRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by codexRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by codexRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val parties by codexRepositories.partyRepository.observeAll().collectAsState(initial = emptyList())
        val species by codexRepositories.speciesRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val pinnedPhrases by phraseSnippetRepository.observeAll().collectAsState(initial = emptyList())
        val frequentPhrases = rememberFrequentPhrases(allEntries)
        val failure = remember { mutableStateOf<Throwable?>(null) }
        val scope = rememberGuardedScope { failure.value = it }

        val currentNote = allSessions.find { it.id == screen.sessionNoteId }
        val numberLabel = allSessions.sessionNumberLabels(numbering)[screen.sessionNoteId]

        val fields = rememberFields(parties.firstOrNull { it.isPlayerCharacter }?.level)
        val highlightedEntryId = remember { mutableStateOf(screen.focusEntryId) }
        LaunchedEffect(highlightedEntryId.value) {
            if (highlightedEntryId.value != null) {
                delay(HIGHLIGHT_DURATION)
                highlightedEntryId.value = null
            }
        }
        fields.toast.autoDismiss()
        ReportFailures(failure, rememberPlaceFailureText()) { fields.toast.value = it }
        // the bar of a made record points into this screen, so it loses that button once the screen is gone
        DisposableEffect(screen.sessionNoteId) { onDispose { undoController.dropFollowUp() } }
        val titleField = fields.titleField
        val draft = fields.composer.draft
        val editingEntryId = fields.composer.editingEntryId
        val selectedEntryId = fields.composer.selectedEntryId
        val selectedEntityRef = fields.selectedEntityRef

        LaunchedEffect(currentNote?.id) {
            if (titleField.value == null && currentNote != null) {
                titleField.value = currentNote.title
            }
        }

        val sessionBodies =
            remember(currentNote, allSessions, entries, allEntries) {
                mentionSessionBodies(currentNote, allSessions, entries, allEntries)
            }
        val mention = buildMentionContext(npcs, locations, quests, parties, species, draft.value.text, sessionBodies)
        // parsing every note is the costly part, so it only reruns when a note or a mention candidate changes
        val noteItems =
            remember(entries, mention.candidates) {
                entries.associate {
                    it.id to
                        it.toItem(mention.candidates)
                }
            }
        val feed = buildFeed(entries, currentNote?.sessionDate?.date, noteItems)
        val focusIndex =
            highlightedEntryId.value?.let { id ->
                feed.indexOfFirst { it is SessionFeedItem.Note && it.entry.id == id }.takeIf { it >= 0 }
            }
        val entryMentions = mentionsIn(entries.map { it.body }, mention.candidates)
        val headerMentions = entryMentions.map { it.toChipItem() }

        val selectedSummary =
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
                species,
            )
        val fullPhoto =
            rememberFullPhoto(selectedSummary, codexRepositories)
        val selectedEntity = fields.speciesNote.withNoteQuantity(selectedSummary?.withFullPhoto(fullPhoto), entries)

        val editingEntryTimeLabel =
            editingEntryId.value?.let { id -> entries.find { it.id == id }?.createdAt?.toDisplayTime() }
        val undoDeletedEntryTitle = stringResource(Res.string.session_detail_undo_deleted_entry_title)
        val titleText = titleField.value ?: currentNote?.title.orEmpty()

        val assignedTags by tagRepository.observeTagsFor(screen.sessionNoteId).collectAsState(initial = emptyList())
        val tagCatalog by tagRepository.observeCatalog().collectAsState(initial = emptyList())
        val mentionedQuestNames = entryMentions.filterIsInstance<MentionEntity.QuestMention>().map { it.name }
        val tagSuggestions =
            suggestSessionTags(
                entries.map { plainMentionText(it.body) },
                mentionedQuestNames,
                assignedTags,
                tagReasonLabels(),
            ).map { SessionTagSuggestionItem(it.tag, it.reason) }
        val tagButtonLabel =
            if (assignedTags.isEmpty()) {
                stringResource(Res.string.session_detail_tag_button_label_empty)
            } else {
                stringResource(Res.string.session_detail_tag_button_label_has_tags)
            }
        val tagSheet =
            if (fields.tags.isOpen.value) {
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
                    draft = fields.tags.draft.value,
                )
            } else {
                null
            }

        val phraseSavedToast = stringResource(Res.string.session_detail_phrase_saved_toast)
        val mentionData = mentionEventData(mention.candidates, mention.entities, allEntries)
        val phrasePool = remember(pinnedPhrases, frequentPhrases) { phrasePool(pinnedPhrases, frequentPhrases) }
        val completions =
            if (mention.suggestions.isEmpty()) phraseCompletions(draft.value.text, phrasePool)?.options else null

        // deliberately renders the same header/feed/composer shape whether or not currentNote
        // has arrived yet from the Flow's cold start - swapping to a distinct "loading" screen
        // for that one frame was the cause of a visible flash on every navigation into this screen
        val campaign by campaignRepository.observeCurrent().collectAsState(initial = null)
        val readOnly = campaign?.status == CampaignStatus.ARCHIVED
        return SessionDetailState(
            readOnly = readOnly,
            isLoading = currentNote == null,
            overline = numberLabel?.let { stringResource(Res.string.session_overline_format, it) }.orEmpty(),
            title = titleText,
            dateLabel = currentNote?.sessionDate?.toDisplayDate(currentAppLanguage()).orEmpty(),
            isLive = currentNote?.isLive ?: true,
            headerMentions = headerMentions,
            feed = feed,
            draft = draft.value.text,
            draftSelection = draft.value.selection,
            quantityHint = pendingQuantityName(draft.value.text, draft.value.selection.end),
            mentionSuggestions = mention.suggestions,
            selectedEntity = selectedEntity,
            editingEntryTimeLabel = editingEntryTimeLabel,
            selectedEntryId = selectedEntryId.value,
            isTitleEditing = fields.titleEditing.value,
            editingEntryId = editingEntryId.value,
            highlightedEntryId = highlightedEntryId.value,
            focusIndex = focusIndex,
            tags = assignedTags,
            tagButtonLabel = tagButtonLabel,
            tagSuggestionCount = tagSuggestions.size,
            tagSheet = tagSheet,
            phraseStrip = phrasePool.take(PHRASE_STRIP_SIZE).map { it.toItem() },
            phraseCompletions = completions.orEmpty().map { it.toItem() },
            phraseSheet = fields.phrases.sheetState(pinnedPhrases, frequentPhrases),
            toast = fields.toast.value,
            deletedMentionSheet = fields.deleted.state(mentionData),
            mentionCreateSheet = fields.create.state(mentionData),
            entryForm = fields.formController.buildState(npcs, locations, species),
        ) { event ->
            val context = EventContext(currentNote, entries, undoDeletedEntryTitle, phraseSavedToast, scope, readOnly)
            onEvent(event, context, fields, assignedTags, tagSuggestions, mentionData)
        }
    }

    // edited locally so live Flow re-emissions from other screens don't clobber in-progress typing
    @Composable
    private fun rememberFields(playerCharacterLevel: Int?): SessionDetailFields {
        val noteId = screen.sessionNoteId
        val selectedEntityRef = remember(noteId) { mutableStateOf<EntityRef?>(null) }
        val entityHistory = remember(noteId) { mutableStateOf(emptyList<EntityRef>()) }
        val recentPlaceIds = remember { mutableStateOf(emptyList<Long>()) }
        val formController =
            CodexEntryFormController.rememberController(
                codexRepositories.npcRepository,
                codexRepositories.partyRepository,
                codexRepositories.questRepository,
                codexRepositories.locationRepository,
                codexRepositories.speciesRepository,
                recentPlaceIds,
                playerCharacterLevel,
            )
        val draft = remember(noteId) { mutableStateOf(TextFieldValue("")) }
        return SessionDetailFields(
            titleField = remember(noteId) { mutableStateOf(null) },
            titleEditing = remember(noteId) { mutableStateOf(false) },
            selectedEntityRef = selectedEntityRef,
            entitySheet = entitySheetInteractions(selectedEntityRef, entityHistory),
            composer =
                SessionDraftController(
                    noteId,
                    sessionEntryRepository,
                    undoController,
                    draft = draft,
                    editingEntryId = remember(noteId) { mutableStateOf(null) },
                    selectedEntryId = remember(noteId) { mutableStateOf(null) },
                ),
            tags =
                SessionTagsController(
                    noteId,
                    tagRepository,
                    isOpen = remember(noteId) { mutableStateOf(false) },
                    draft = remember(noteId) { mutableStateOf("") },
                ),
            deleted =
                DeletedMentionController(
                    remember(noteId) { mutableStateOf(null) },
                    mentionActions,
                    formController,
                ),
            create =
                MentionCreateController(
                    remember(noteId) { mutableStateOf(null) },
                    draft,
                    codexRepositories,
                    undoController,
                    appPreferencesRepository,
                    mentionActions,
                    formController,
                ) { selectedEntityRef.value = it },
            phrases = remember { PhraseSheetController(phraseSnippetRepository) },
            speciesNote = SpeciesNoteController(remember(noteId) { mutableStateOf(null) }, sessionEntryRepository),
            toast = remember(noteId) { mutableStateOf(null) },
            formController = formController,
        )
    }

    private fun entitySheetInteractions(
        selectedEntityRef: MutableState<EntityRef?>,
        entityHistory: MutableState<List<EntityRef>>,
    ) = EntitySheetInteractions(
        selectedEntityRef,
        codexRepositories,
        navigator.sessionEntryOpener(),
        entityHistory,
    )

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
                fields.entitySheet.onEvent(scope, event)
            }
            else -> fields.entitySheet.onEvent(scope, event)
        }
    }

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun onEvent(
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
            is SessionDetailEvent.DraftChanged -> fields.composer.onDraftChanged(event.text, event.selection)
            SessionDetailEvent.InsertMentionTriggerClicked -> fields.composer.insertMentionTrigger()
            is SessionDetailEvent.MentionSuggestionPicked -> fields.composer.pickMention(event.token)
            is SessionDetailEvent.EntitySheet -> onEntitySheet(event.event, entries, fields, scope)
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

/** everything an event handler reads besides the editable fields - one value instead of five parameters */
private data class EventContext(
    val currentNote: SessionNote?,
    val entries: List<SessionEntry>,
    val undoDeletedEntryTitle: String,
    val phraseSavedToast: String,
    val scope: CoroutineScope,
    val readOnly: Boolean,
)

private fun PhraseOption.toItem() =
    SessionPhraseItem(
        text = text,
        label = phraseLabel(text),
        isPinned = isPinned,
        entryCount = entryCount,
    )

@Composable
private fun tagReasonLabels() =
    TagReasonLabels(
        quest = stringResource(Res.string.session_tag_reason_quest),
        quests = stringResource(Res.string.session_tag_reason_quests),
        words = stringResource(Res.string.session_tag_reason_words),
    )

/** the notes of this session and of the session played just before it - what the empty "@" dropdown draws from */
private fun mentionSessionBodies(
    currentNote: SessionNote?,
    allSessions: List<SessionNote>,
    entries: List<SessionEntry>,
    allEntries: List<SessionEntry>,
): MentionSessionBodies {
    val previousId =
        currentNote?.let { note ->
            allSessions
                .filter { it.id != note.id && it.sessionDate <= note.sessionDate }
                .maxByOrNull { it.sessionDate }
                ?.id
        }
    return MentionSessionBodies(
        current = entries.map { it.body },
        previous = allEntries.filter { it.sessionNoteId == previousId }.map { it.body },
    )
}
