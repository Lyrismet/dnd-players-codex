package com.lyrismet.dndcodex.presentation.sessiondetail

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
import com.lyrismet.dndcodex.core.designsystem.LocationMentionColor
import com.lyrismet.dndcodex.core.designsystem.component.MentionChipItem
import com.lyrismet.dndcodex.core.designsystem.component.MentionGlyph
import com.lyrismet.dndcodex.core.designsystem.component.toChipItem
import com.lyrismet.dndcodex.core.designsystem.toStatusColor
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import com.lyrismet.dndcodex.core.entitysummary.EntitySheetInteractions
import com.lyrismet.dndcodex.core.entitysummary.selectedEntitySummary
import com.lyrismet.dndcodex.core.format.chronologicalNumberLabels
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toDisplayTime
import com.lyrismet.dndcodex.core.format.toShortDayMonthUpper
import com.lyrismet.dndcodex.core.mention.MentionCandidate
import com.lyrismet.dndcodex.core.mention.MentionEntity
import com.lyrismet.dndcodex.core.mention.MentionSegment
import com.lyrismet.dndcodex.core.mention.insertMention
import com.lyrismet.dndcodex.core.mention.matchingMentionEntities
import com.lyrismet.dndcodex.core.mention.mentionCandidates
import com.lyrismet.dndcodex.core.mention.mentionEntitiesFrom
import com.lyrismet.dndcodex.core.mention.mentionKey
import com.lyrismet.dndcodex.core.mention.mentionsIn
import com.lyrismet.dndcodex.core.mention.parseMentions
import com.lyrismet.dndcodex.core.mention.trailingMentionQuery
import com.lyrismet.dndcodex.domain.model.SessionEntry
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_meeting_separator_format
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import dndplayerscodex.shared.generated.resources.session_overline_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
private class SessionDetailFields(
    val titleField: MutableState<String?>,
    val draft: MutableState<TextFieldValue>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
)

class SessionDetailPresenter(
    private val screen: SessionDetailScreen,
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val mentionRepositories: MentionRepositories,
) : Presenter<SessionDetailState> {
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
        val scope = rememberCoroutineScope()

        val currentNote = allSessions.find { it.id == screen.sessionNoteId }
        val numberLabel = numberLabelFor(allSessions)

        // edited locally so live Flow re-emissions from other screens don't clobber in-progress typing
        val titleField = remember(screen.sessionNoteId) { mutableStateOf<String?>(null) }
        val draft = remember(screen.sessionNoteId) { mutableStateOf(TextFieldValue("")) }
        val selectedEntityRef = remember(screen.sessionNoteId) { mutableStateOf<EntityRef?>(null) }
        val fields =
            SessionDetailFields(titleField, draft, selectedEntityRef, entitySheetInteractions(selectedEntityRef))

        LaunchedEffect(currentNote?.id) {
            if (titleField.value == null && currentNote != null) {
                titleField.value = currentNote.title
            }
        }

        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val mentionEntities = mentionEntitiesFrom(npcs, locations, quests)
        val candidates = mentionCandidates(mentionEntities, questPrefix)

        val npcTypeLabel = stringResource(Res.string.session_detail_mention_type_npc)
        val locationTypeLabel = stringResource(Res.string.session_detail_mention_type_location)
        val questTypeLabel = stringResource(Res.string.session_detail_mention_type_quest)

        val feed = buildFeed(entries, currentNote?.sessionDate?.date, candidates)
        val headerMentions = headerMentions(entries, candidates)

        val suggestions =
            trailingMentionQuery(draft.value.text)
                ?.let { matchingMentionEntities(mentionEntities, it, questPrefix) }
                .orEmpty()
                .map { it.toSuggestion(questPrefix, npcTypeLabel, locationTypeLabel, questTypeLabel) }

        val selectedEntity =
            selectedEntitySummary(
                selectedEntityRef.value,
                npcs,
                locations,
                quests,
                allSessions,
                allEntries,
                candidates,
            )

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
            mentionSuggestions = suggestions,
            selectedEntity = selectedEntity,
        ) { event -> onEvent(event, currentNote, scope, fields) }
    }

    private fun numberLabelFor(allSessions: List<SessionNote>): String? =
        allSessions.chronologicalNumberLabels()[screen.sessionNoteId]

    private fun entitySheetInteractions(selectedEntityRef: MutableState<EntityRef?>) =
        EntitySheetInteractions(
            selectedEntityRef,
            mentionRepositories.npcRepository,
            mentionRepositories.questRepository,
            navigator,
        )

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod")
    private fun onEvent(
        event: SessionDetailEvent,
        currentNote: SessionNote?,
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
            is SessionDetailEvent.QuestStatusSelected ->
                fields.entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is SessionDetailEvent.RelatedNoteClicked -> fields.entitySheet.onRelatedNoteClicked(event.sessionNoteId)
            SessionDetailEvent.SheetDismissed -> fields.entitySheet.onDismissed()
            SessionDetailEvent.SubmitEntryClicked -> onSubmitEntry(scope, fields.draft)
            is SessionDetailEvent.DeleteEntryClicked -> scope.launch { sessionEntryRepository.delete(event.id) }
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
        draft: MutableState<TextFieldValue>,
    ) {
        val body = draft.value.text.trim()
        if (body.isNotEmpty()) {
            draft.value = TextFieldValue("")
            scope.launch { sessionEntryRepository.add(screen.sessionNoteId, body) }
        }
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

    private fun onResumeSession(
        currentNote: SessionNote?,
        scope: CoroutineScope,
    ) {
        currentNote?.let { note -> scope.launch { sessionNoteRepository.upsert(note.copy(endedAt = null)) } }
    }
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
