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
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
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
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_meeting_separator_format
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import dndplayerscodex.shared.generated.resources.session_detail_undo_deleted_entry_title
import dndplayerscodex.shared.generated.resources.session_overline_format
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

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
private class SessionDetailFields(
    val titleField: MutableState<String?>,
    val draft: MutableState<TextFieldValue>,
    val editingEntryId: MutableState<Long?>,
    val selectedEntryId: MutableState<Long?>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
)

@Suppress("LongParameterList")
class SessionDetailPresenter(
    private val screen: SessionDetailScreen,
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val partyRepository: PartyRepository,
    private val mentionRepositories: MentionRepositories,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
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
        val headerMentions = headerMentions(entries, mention.candidates)

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

        // deliberately renders the same header/feed/composer shape whether or not currentNote
        // has arrived yet from the Flow's cold start - swapping to a distinct "loading" screen
        // for that one frame was the cause of a visible flash on every navigation into this screen
        return SessionDetailState(
            isLoading = currentNote == null,
            overline = numberLabel?.let { stringResource(Res.string.session_overline_format, it) }.orEmpty(),
            title = titleField.value ?: currentNote?.title.orEmpty(),
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
        ) { event -> onEvent(event, currentNote, entries, undoDeletedEntryTitle, scope, fields) }
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
    @Suppress("CyclomaticComplexMethod", "LongParameterList")
    private fun onEvent(
        event: SessionDetailEvent,
        currentNote: SessionNote?,
        entries: List<SessionEntry>,
        undoDeletedEntryTitle: String,
        scope: CoroutineScope,
        fields: SessionDetailFields,
    ) {
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
            SessionDetailEvent.SubmitEntryClicked -> onSubmitEntry(scope, fields)
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
        }
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
        fields: SessionDetailFields,
    ) {
        val body =
            fields.draft.value.text
                .trim()
        if (body.isEmpty()) return
        val editingId = fields.editingEntryId.value
        fields.draft.value = TextFieldValue("")
        fields.editingEntryId.value = null
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

/** every distinct entity mentioned anywhere in the session, in first-seen order - no cap, unlike the list card */
private fun headerMentions(
    entries: List<SessionEntry>,
    candidates: List<MentionCandidate>,
): List<MentionChipItem> = mentionsIn(entries.map { it.body }, candidates).map { it.toChipItem() }

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
