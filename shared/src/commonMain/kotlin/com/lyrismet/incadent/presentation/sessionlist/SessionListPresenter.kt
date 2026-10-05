package com.lyrismet.incadent.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.designsystem.component.toChipItem
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.format.chronologicalIndex
import com.lyrismet.incadent.core.format.sessionNumberLabels
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.mention.mentionsIn
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.default_campaign_name
import dndplayerscodex.shared.generated.resources.new_session_default_title
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import dndplayerscodex.shared.generated.resources.session_list_live_no_notes
import dndplayerscodex.shared.generated.resources.session_list_live_notes_summary_format
import dndplayerscodex.shared.generated.resources.session_list_undo_deleted_title
import dndplayerscodex.shared.generated.resources.session_overline_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("LongParameterList")
class SessionListPresenter(
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val partyRepository: PartyRepository,
    private val mentionRepositories: MentionRepositories,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter<SessionListState> {
    @Composable
    override fun present(): SessionListState {
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val npcs by mentionRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by mentionRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by mentionRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val parties by partyRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val campaignNameOverride by appPreferencesRepository.observeCampaignName().collectAsState(initial = null)
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val defaultCampaignName = stringResource(Res.string.default_campaign_name)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val noNotesLabel = stringResource(Res.string.session_list_live_no_notes)
        val undoDeletedTitle = stringResource(Res.string.session_list_undo_deleted_title)
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), questPrefix)

        val liveNote = sessions.firstOrNull { it.isLive }
        val liveEntriesFlow: Flow<List<SessionEntry>> =
            remember(liveNote?.id) {
                liveNote?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
            }
        val liveEntries by liveEntriesFlow.collectAsState(initial = emptyList())

        val allItems = sessions.toListItems(numbering)
        val liveItem = liveNote?.let { note -> allItems.first { it.id == note.id } }
        val liveSession = liveItem?.let { item -> buildLiveSession(item, liveEntries, candidates, noNotesLabel) }

        val selectedEntityRef = remember { mutableStateOf<EntityRef?>(null) }
        val entitySheet =
            EntitySheetInteractions(
                selectedEntityRef,
                mentionRepositories.npcRepository,
                mentionRepositories.questRepository,
                partyRepository,
                navigator,
            )
        val selectedEntity =
            selectedEntitySummary(
                selectedEntityRef.value,
                npcs,
                locations,
                quests,
                sessions,
                allEntries,
                candidates,
                numbering,
                parties,
            )

        return SessionListState(
            campaignName = campaignNameOverride ?: defaultCampaignName,
            liveSession = liveSession,
            sessions = if (liveNote != null) allItems.filterNot { it.id == liveNote.id } else allItems,
            selectedEntity = selectedEntity,
        ) { event -> onEvent(event, scope, newSessionTitle, undoDeletedTitle, sessions, entitySheet, numbering) }
    }

    @Composable
    private fun buildLiveSession(
        item: SessionListItem,
        liveEntries: List<SessionEntry>,
        candidates: List<MentionCandidate>,
        noNotesLabel: String,
    ): LiveSessionItem {
        // reversed so a dedupe-by-first-seen keeps each entity's most recent mention, not its oldest one
        val mentions =
            mentionsIn(liveEntries.asReversed().map { it.body }, candidates).take(5).map { it.toChipItem() }
        val notesSummary =
            liveEntries.lastOrNull()?.let { last ->
                stringResource(
                    Res.string.session_list_live_notes_summary_format,
                    liveEntries.size,
                    last.createdAt.toDisplayTime(),
                )
            } ?: noNotesLabel
        return LiveSessionItem(
            id = item.id,
            numberLabel = item.numberLabel,
            // the design keeps this sub-label arabic even though the glyph above and the detail screen stay roman
            overline = stringResource(Res.string.session_overline_format, item.arabicNumber),
            title = item.title,
            dateLabel = item.dateLabel,
            notesSummary = notesSummary,
            mentions = mentions,
        )
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("LongParameterList")
    private fun onEvent(
        event: SessionListEvent,
        scope: CoroutineScope,
        newSessionTitle: String,
        undoDeletedTitle: String,
        sessions: List<SessionNote>,
        entitySheet: EntitySheetInteractions,
        numbering: SessionNumbering,
    ) {
        when (event) {
            SessionListEvent.NewSessionClicked ->
                scope.launch {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val id =
                        sessionNoteRepository.upsert(
                            SessionNote(id = 0, title = newSessionTitle, sessionDate = now, endedAt = null),
                        )
                    // only one session may be live at a time - starting a new one closes out the old one
                    sessionNoteRepository.endOtherLiveSessions(exceptId = id, endedAt = now)
                    navigator.goTo(SessionDetailScreen(id))
                }

            is SessionListEvent.SessionClicked -> navigator.goTo(SessionDetailScreen(event.id))
            is SessionListEvent.DeleteSessionClicked ->
                onDeleteSessionClicked(event.id, sessions, undoDeletedTitle, scope, numbering)
            is SessionListEvent.MentionChipClicked -> entitySheet.onEntityClicked(event.ref)
            is SessionListEvent.NpcStatusSelected ->
                entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is SessionListEvent.NpcLifeSelected ->
                entitySheet.onNpcLifeSelected(scope, event.npcId, event.lifeState)
            is SessionListEvent.QuestStatusSelected ->
                entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is SessionListEvent.RelatedNoteClicked -> entitySheet.onRelatedNoteClicked(event.sessionNoteId)
            SessionListEvent.SheetDismissed -> entitySheet.onDismissed()
        }
    }

    // deletes immediately and offers undo - restoring re-inserts the note and all its entries with fresh ids
    @OptIn(ExperimentalTime::class)
    private fun onDeleteSessionClicked(
        id: Long,
        sessions: List<SessionNote>,
        undoDeletedTitle: String,
        scope: CoroutineScope,
        numbering: SessionNumbering,
    ) {
        val note = sessions.find { it.id == id } ?: return
        val numLabel = sessions.sessionNumberLabels(numbering)[id].orEmpty()
        scope.launch {
            val entries = sessionEntryRepository.observeForSession(id).first()
            sessionNoteRepository.delete(id)
            undoController.show(undoDeletedTitle, "$numLabel · ${note.title}") {
                // another session may have gone live meanwhile, so the restored one comes back ended
                val anotherIsLive = sessionNoteRepository.observeAll().first().any { it.isLive }
                val restored =
                    if (note.isLive && anotherIsLive) {
                        note.copy(id = 0, endedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                    } else {
                        note.copy(id = 0)
                    }
                val newId = sessionNoteRepository.upsert(restored)
                entries.forEach { entry -> sessionEntryRepository.restore(entry.copy(sessionNoteId = newId)) }
            }
        }
    }

    // numbered by chronological order (oldest = I) even though [this] arrives newest-first from the repo
    private fun List<SessionNote>.toListItems(numbering: SessionNumbering): List<SessionListItem> {
        val numberLabels = sessionNumberLabels(numbering)
        val arabicNumbers = chronologicalIndex()
        return map { note ->
            SessionListItem(
                id = note.id,
                numberLabel = numberLabels.getValue(note.id),
                arabicNumber = arabicNumbers.getValue(note.id),
                title = note.title,
                dateLabel = note.sessionDate.toDisplayDate(),
            )
        }
    }
}
