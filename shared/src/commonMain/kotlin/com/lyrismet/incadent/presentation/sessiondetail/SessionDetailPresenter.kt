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
import com.lyrismet.incadent.core.designsystem.PartyMemberColor
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.designsystem.component.toChipItem
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.rememberFullPhoto
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.entitysummary.withFullPhoto
import com.lyrismet.incadent.core.format.sessionNumberLabels
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.format.toShortDayMonthUpper
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.MentionSegment
import com.lyrismet.incadent.core.mention.autoMatchedEntities
import com.lyrismet.incadent.core.mention.dedupeKey
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
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.tags.TagReasonLabels
import com.lyrismet.incadent.core.tags.normalizeCustomTagName
import com.lyrismet.incadent.core.tags.suggestSessionTags
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.CampaignStatus
import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.DeletedMentionRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.PhraseSnippetRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
import com.lyrismet.incadent.presentation.codex.CodexEntryFormController
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
import org.jetbrains.compose.resources.getString
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
    private val partyRepository: PartyRepository,
    private val mentionRepositories: MentionRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val phraseSnippetRepository: PhraseSnippetRepository,
    private val deletedMentionRepository: DeletedMentionRepository,
    private val imageCompressor: ImageCompressor,
    private val campaignRepository: CampaignRepository,
) : Presenter<SessionDetailState> {
    private val mentionActions = SessionMentionActions(sessionEntryRepository, deletedMentionRepository, undoController)

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
        val deleted by deletedMentionRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val pinnedPhrases by phraseSnippetRepository.observeAll().collectAsState(initial = emptyList())
        val frequentPhrases = rememberFrequentPhrases(allEntries)
        val scope = rememberCoroutineScope()

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

        val mention = buildMentionContext(npcs, locations, quests, parties, deleted, draft.value.text)
        // a record created again takes its mentions back, so its snapshot has nothing left to stand in for
        ForgetRevivedMentions(mention.revived, deletedMentionRepository)
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
            rememberFullPhoto(
                selectedSummary,
                mentionRepositories.npcRepository,
                partyRepository,
                mentionRepositories.locationRepository,
            )
        val selectedEntity = selectedSummary?.withFullPhoto(fullPhoto)

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
        val mentionData = mentionEventData(mention.candidates, mention.entities, allEntries, deleted)
        val phrasePool = remember(pinnedPhrases, frequentPhrases) { phrasePool(pinnedPhrases, frequentPhrases) }
        val completions =
            if (mention.suggestions.isEmpty()) phraseCompletions(draft.value.text, phrasePool)?.options else null

        // deliberately renders the same header/feed/composer shape whether or not currentNote
        // has arrived yet from the Flow's cold start - swapping to a distinct "loading" screen
        // for that one frame was the cause of a visible flash on every navigation into this screen
        val campaign by campaignRepository.observeCurrent().collectAsState(initial = null)
        return SessionDetailState(
            readOnly = campaign?.status == CampaignStatus.ARCHIVED,
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
            deletedMentionSheet = deletedSheet(fields.deletedSheet.value, mentionData),
            entryForm = fields.formController.buildState(npcs, locations),
        ) { event ->
            val context = EventContext(currentNote, entries, undoDeletedEntryTitle, phraseSavedToast, scope)
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
                mentionRepositories.npcRepository,
                partyRepository,
                mentionRepositories.questRepository,
                mentionRepositories.locationRepository,
                recentPlaceIds,
                playerCharacterLevel,
            )
        return SessionDetailFields(
            titleField = remember(noteId) { mutableStateOf<String?>(null) },
            draft = remember(noteId) { mutableStateOf(TextFieldValue("")) },
            editingEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
            selectedEntryId = remember(noteId) { mutableStateOf<Long?>(null) },
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
        mentionRepositories,
        partyRepository,
        navigator,
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
            SessionDetailEvent.EntrySelectionCleared -> fields.selectedEntryId.value = null
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
            SessionDetailEvent.DeletedMentionCreateClicked -> onDeletedMentionCreate(fields, mentionData)
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
                    mentionData.candidates
                        .map { it.entity }
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
    private fun onDeletedMentionCreate(
        fields: SessionDetailFields,
        data: MentionEventData,
    ) {
        val open = fields.deletedSheet.value ?: return
        val deleted = data.deletedMatching(open.deleted) ?: return
        fields.deletedSheet.value = null
        fields.formController.onAddEntryClicked(deleted.type.toEntryType(), deleted.name)
    }

    // a null [key] keeps the name as plain words, otherwise the mention points at the picked record
    private fun onDeletedMentionRewrite(
        key: String?,
        fields: SessionDetailFields,
        data: MentionEventData,
        scope: CoroutineScope,
    ) {
        val deleted = fields.deletedSheet.value?.let { data.deletedMatching(it.deleted) } ?: return
        val target = key?.let { id -> data.linkTargets.firstOrNull { it.id == id }?.entity }
        // a picked record that is gone by now leaves the tag alone instead of turning it into words
        if (key == null || target != null) {
            fields.deletedSheet.value = null
            mentionActions.rewriteDeleted(scope, deleted, target, data)
        }
    }

    // the sheet only exists while its snapshot does, so a tag that was linked or revived closes it by itself
    private fun deletedSheet(
        open: OpenDeletedSheet?,
        data: MentionEventData,
    ): DeletedMentionSheetState? {
        val deleted = open?.let { data.deletedMatching(it.deleted) } ?: return null
        val hits = data.hitsOf(deleted)
        return DeletedMentionSheetState(
            name = deleted.name,
            type = deleted.type,
            notes = hits.notes,
            sessions = hits.sessions,
            isPicking = open.isPicking,
            query = open.query,
            linkOptions = if (open.isPicking) data.linkOptionsFor(open.query) else emptyList(),
        )
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
    // the live records, in the order the "link to" list offers them
    val entities: List<MentionEntity>,
    // snapshots whose name a live record has taken again
    val revived: List<DeletedMention>,
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
    parties: List<PartyMember>,
    deleted: List<DeletedMention>,
    draftText: String,
): MentionContext {
    val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
    val mentionEntities = mentionEntitiesFrom(npcs, locations, quests, parties)
    val candidates = mentionCandidates(mentionEntities, questPrefix, deleted)
    val liveKeys = mentionEntities.mapTo(HashSet()) { mentionKey(it, questPrefix) }
    val revived =
        deleted
            .filter { mentionKey(MentionEntity.DeletedMention(it.name, it.type), questPrefix) in liveKeys }

    val typeLabels =
        MentionTypeLabels(
            npc = stringResource(Res.string.session_detail_mention_type_npc),
            party = stringResource(Res.string.session_detail_mention_type_party),
            location = stringResource(Res.string.session_detail_mention_type_location),
            quest = stringResource(Res.string.session_detail_mention_type_quest),
        )
    val suggestions =
        trailingMentionQuery(draftText)
            ?.let { matchingMentionEntities(mentionEntities, it, questPrefix) }
            .orEmpty()
            .map { it.toSuggestion(questPrefix, typeLabels) }

    return MentionContext(candidates, suggestions, mentionEntities, revived)
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

private fun SessionEntry.toItem(candidates: List<MentionCandidate>): SessionEntryItem {
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

private fun MentionEntity.toSuggestion(
    questPrefix: String,
    labels: MentionTypeLabels,
): SessionMentionSuggestion {
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
