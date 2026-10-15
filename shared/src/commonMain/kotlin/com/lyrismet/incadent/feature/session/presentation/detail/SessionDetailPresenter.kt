package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.errors.ReportFailures
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.format.toShortDayMonthUpper
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.style.LocationMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.onEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.rememberFullPhoto
import com.lyrismet.incadent.feature.codex.presentation.summary.selectedEntitySummary
import com.lyrismet.incadent.feature.codex.presentation.summary.withFullPhoto
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.domain.autoMatchedEntities
import com.lyrismet.incadent.feature.mention.domain.dedupeKey
import com.lyrismet.incadent.feature.mention.domain.insertMention
import com.lyrismet.incadent.feature.mention.domain.linkTypedMentions
import com.lyrismet.incadent.feature.mention.domain.matchingMentionEntities
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.mentionsIn
import com.lyrismet.incadent.feature.mention.domain.parseMentions
import com.lyrismet.incadent.feature.mention.domain.plainMentionText
import com.lyrismet.incadent.feature.mention.domain.token
import com.lyrismet.incadent.feature.mention.domain.trailingMentionQuery
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.phrases.domain.PhraseOption
import com.lyrismet.incadent.feature.phrases.domain.insertPhrase
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
import com.lyrismet.incadent.feature.session.domain.tags.normalizeCustomTagName
import com.lyrismet.incadent.feature.session.domain.tags.suggestSessionTags
import com.lyrismet.incadent.feature.session.presentation.route.SessionDetailScreen
import com.lyrismet.incadent.feature.session.presentation.route.sessionEntryOpener
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_toast_format
import dndplayerscodex.shared.generated.resources.session_detail_meeting_separator_format
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_party
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.lyrismet.incadent.feature.mention.domain.withoutBrokenTokens as stripBrokenTokens

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
    val titleEditing: MutableState<Boolean>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
    val tagSheetOpen: MutableState<Boolean>,
    val tagDraft: MutableState<String>,
    val phrases: PhraseSheetController,
    val toast: MutableState<String?>,
    // the "?" tag whose sheet is open, and whether its link-to-another-record list is showing
    val deletedSheet: MutableState<OpenDeletedSheet?>,
    val formController: CodexEntryFormController,
)

/** the "?" tag sheet that is open - [isPicking] swaps its actions for the list of records to link to */
internal data class OpenDeletedSheet(
    val deleted: DeletedMention,
    val isPicking: Boolean = false,
    val query: String = "",
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
    @OptIn(ExperimentalTime::class)
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
                delay(HIGHLIGHT_DURATION_MS)
                highlightedEntryId.value = null
            }
        }
        fields.toast.autoDismiss()
        ReportFailures(failure) { fields.toast.value = it }
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

        val mention = buildMentionContext(npcs, locations, quests, parties, draft.value.text)
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
            )
        val fullPhoto =
            rememberFullPhoto(selectedSummary, codexRepositories)
        val selectedEntity = selectedSummary?.withFullPhoto(fullPhoto)

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
            deletedMentionSheet = deletedSheet(fields.deletedSheet.value, mentionData),
            entryForm = fields.formController.buildState(npcs, locations),
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
                recentPlaceIds,
                playerCharacterLevel,
            )
        return SessionDetailFields(
            titleField = remember(noteId) { mutableStateOf<String?>(null) },
            draft = remember(noteId) { mutableStateOf(TextFieldValue("")) },
            editingEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
            selectedEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
            titleEditing = remember(noteId) { mutableStateOf(false) },
            selectedEntityRef = selectedEntityRef,
            entitySheet = entitySheetInteractions(selectedEntityRef, entityHistory),
            tagSheetOpen = remember(noteId) { mutableStateOf(false) },
            tagDraft = remember(noteId) { mutableStateOf("") },
            phrases = remember { PhraseSheetController(phraseSnippetRepository) },
            toast = remember(noteId) { mutableStateOf<String?>(null) },
            deletedSheet = remember(noteId) { mutableStateOf<OpenDeletedSheet?>(null) },
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
            is SessionDetailEvent.DraftChanged -> fields.draft.value = withoutBrokenTokens(event.text, event.selection)
            SessionDetailEvent.InsertMentionTriggerClicked -> onInsertMentionTrigger(fields.draft)
            is SessionDetailEvent.MentionSuggestionPicked ->
                onMentionSuggestionPicked(event.token, fields.draft)
            is SessionDetailEvent.EntitySheet -> fields.entitySheet.onEvent(scope, event.event)
            SessionDetailEvent.SubmitEntryClicked -> onSubmitEntry(scope, entries, fields, mentionData.candidates)
            is SessionDetailEvent.EntryClicked -> {
                onEntryClicked(event.id, fields.selectedEntryId)
                // the note's action row and the title editor never show together
                if (fields.selectedEntryId.value != null) fields.titleEditing.value = false
            }
            SessionDetailEvent.TitleEditStarted -> {
                fields.titleEditing.value = true
                fields.selectedEntryId.value = null
            }
            SessionDetailEvent.TitleEditFinished -> fields.titleEditing.value = false
            SessionDetailEvent.OutsideTapped -> {
                fields.selectedEntryId.value = null
                fields.titleEditing.value = false
            }
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
            is SessionDetailEvent.DeletedMentionClicked -> fields.deletedSheet.value = OpenDeletedSheet(event.deleted)
            SessionDetailEvent.DeletedMentionSheetDismissed -> fields.deletedSheet.value = null
            is SessionDetailEvent.DeletedMentionPickingChanged ->
                fields.deletedSheet.value = fields.deletedSheet.value?.copy(isPicking = event.isPicking, query = "")
            is SessionDetailEvent.DeletedMentionQueryChanged ->
                fields.deletedSheet.value = fields.deletedSheet.value?.copy(query = event.query)
            SessionDetailEvent.DeletedMentionCreateClicked -> onDeletedMentionCreate(fields)
            is SessionDetailEvent.DeletedMentionLinked ->
                onDeletedMentionRewrite(event.key, fields, mentionData, scope)
            SessionDetailEvent.DeletedMentionKeptAsText -> onDeletedMentionRewrite(null, fields, mentionData, scope)
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

    // the sheet closes and the codex form opens over the screen, named and typed like the tag - nothing is created yet
    private fun onDeletedMentionCreate(fields: SessionDetailFields) {
        val deleted = fields.deletedSheet.value?.deleted ?: return
        fields.deletedSheet.value = null
        // created under the old id, so the tokens already in the notes point at it again
        fields.formController.onAddEntryClicked(deleted.type, deleted.name, reviveId = deleted.id)
    }

    // a null [key] keeps the name as plain words, otherwise the mention points at the picked record
    private fun onDeletedMentionRewrite(
        key: String?,
        fields: SessionDetailFields,
        data: MentionEventData,
        scope: CoroutineScope,
    ) {
        val deleted = fields.deletedSheet.value?.deleted ?: return
        val target = key?.let { id -> data.linkTargets.firstOrNull { it.id == id }?.entity }
        // a picked record that is gone by now leaves the tag alone instead of turning it into words
        if (key == null || target != null) {
            fields.deletedSheet.value = null
            mentionActions.rewriteDeleted(scope, deleted, target, data)
        }
    }

    // the sheet only exists while the tag does, so one that was linked, turned into words or revived closes it
    private fun deletedSheet(
        open: OpenDeletedSheet?,
        data: MentionEventData,
    ): DeletedMentionSheetState? {
        val deleted = open?.deleted ?: return null
        val hits = data.hitsOf(deleted)
        val tagStillThere = hits.notes > 0 && !data.candidates.has(MentionRef(deleted.type, deleted.id))
        return DeletedMentionSheetState(
            name = deleted.name,
            type = deleted.type,
            notes = hits.notes,
            sessions = hits.sessions,
            isPicking = open.isPicking,
            query = open.query,
            linkOptions = if (open.isPicking) data.linkOptionsFor(open.query) else emptyList(),
        ).takeIf { tagStillThere }
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
        token: String,
        draft: MutableState<TextFieldValue>,
    ) {
        val next = insertMention(draft.value.text, token)
        draft.value = TextFieldValue(next, TextRange(next.length))
    }

    private fun onSubmitEntry(
        scope: CoroutineScope,
        entries: List<SessionEntry>,
        fields: SessionDetailFields,
        index: MentionIndex,
    ) {
        // a name typed after "@" without picking it from the list still turns into the token of its record
        val body =
            linkTypedMentions(
                fields.draft.value.text
                    .trim(),
                index,
            )
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
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                sessionNoteRepository.makeOnlyLive(note.id, now)
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
    val readOnly: Boolean,
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

private fun PhraseOption.toItem() =
    SessionPhraseItem(
        text = text,
        label = phraseLabel(text),
        isPinned = isPinned,
        entryCount = entryCount,
    )

private data class MentionContext(
    val candidates: MentionIndex,
    val suggestions: List<SessionMentionSuggestion>,
    // the live records, in the order the "link to" list offers them
    val entities: List<MentionEntity>,
    // snapshots whose name a live record has taken again
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
        val plain = plainMentionText(entry.body)
        val preview = plain.take(ENTRY_PREVIEW_LENGTH)
        val suffix = if (plain.length > ENTRY_PREVIEW_LENGTH) "…" else ""
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
    parties: List<PartyMember>,
    draftText: String,
): MentionContext {
    val mentionEntities = mentionEntitiesFrom(npcs, locations, quests, parties)
    val candidates = MentionIndex(mentionEntities)

    val typeLabels =
        MentionTypeLabels(
            npc = stringResource(Res.string.session_detail_mention_type_npc),
            party = stringResource(Res.string.session_detail_mention_type_party),
            location = stringResource(Res.string.session_detail_mention_type_location),
            quest = stringResource(Res.string.session_detail_mention_type_quest),
        )
    val suggestions =
        trailingMentionQuery(draftText)
            ?.let { matchingMentionEntities(mentionEntities, it) }
            .orEmpty()
            .map { it.toSuggestion(typeLabels) }

    return MentionContext(candidates, suggestions, mentionEntities)
}

/** groups [entries] under a "ВСТРЕЧА N" separator per calendar day, only when the session spans more than one */
@Composable
private fun buildFeed(
    entries: List<SessionEntry>,
    sessionStartDay: LocalDate?,
    noteItems: Map<Long, SessionEntryItem>,
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
        noteItems[entry.id]?.let { feed += SessionFeedItem.Note(it) }
    }
    return feed
}

private fun SessionEntry.toItem(candidates: MentionIndex): SessionEntryItem {
    // only a note that was sent or edited and not decided on yet is searched for words without "@"
    val parsed = parseMentions(body, candidates, autoMatch = mentionCheck)
    return SessionEntryItem(
        id = id,
        timeLabel = createdAt.toDisplayTime(),
        edited = edited,
        segments = parsed.map { it.toSegment() },
        autoMatchCount = autoMatchedEntities(parsed).size,
    )
}

private fun MentionSegment.toSegment(): SessionEntrySegment =
    when (this) {
        is MentionSegment.Text -> SessionEntrySegment.Text(text)
        is MentionSegment.Mention -> SessionEntrySegment.Mention(entity.toChipItem())
        is MentionSegment.AutoMatch -> SessionEntrySegment.AutoMatch(text, entity.dedupeKey())
    }

/** the name of each record kind the autocomplete shows next to a suggestion */
private class MentionTypeLabels(
    val npc: String,
    val party: String,
    val location: String,
    val quest: String,
)

private fun MentionEntity.toSuggestion(labels: MentionTypeLabels): SessionMentionSuggestion {
    val (glyph, tint, typeLabel) =
        when (this) {
            is MentionEntity.NpcMention -> Triple(MentionGlyph.NPC, status.toStatusColor().foreground, labels.npc)
            is MentionEntity.LocationMention ->
                Triple(MentionGlyph.LOCATION, LocationMentionColor.foreground, labels.location)
            is MentionEntity.QuestMention ->
                Triple(MentionGlyph.QUEST, status.toStatusColor().foreground, labels.quest)
            is MentionEntity.PartyMention -> Triple(MentionGlyph.PARTY, PartyMemberColor.foreground, labels.party)
            // a deleted record is never offered by the autocomplete, its candidates come from live records only
            is MentionEntity.DeletedMention -> Triple(MentionGlyph.DELETED, LocationMentionColor.foreground, "")
        }
    return SessionMentionSuggestion(
        token = token(),
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

// a cut token goes whole, the caret lands where the same text position now is
private fun withoutBrokenTokens(
    text: String,
    selection: TextRange,
): TextFieldValue {
    val clean = stripBrokenTokens(text)
    if (clean == text) return TextFieldValue(text, selection)
    val caret = stripBrokenTokens(text.take(selection.end)).length
    return TextFieldValue(clean, TextRange(caret.coerceAtMost(clean.length)))
}
