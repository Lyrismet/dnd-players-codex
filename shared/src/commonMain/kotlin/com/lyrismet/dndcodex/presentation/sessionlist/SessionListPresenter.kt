package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.core.designsystem.component.toChipItem
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import com.lyrismet.dndcodex.core.entitysummary.EntitySheetInteractions
import com.lyrismet.dndcodex.core.entitysummary.selectedEntitySummary
import com.lyrismet.dndcodex.core.format.chronologicalIndex
import com.lyrismet.dndcodex.core.format.chronologicalNumberLabels
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toDisplayTime
import com.lyrismet.dndcodex.core.mention.MentionCandidate
import com.lyrismet.dndcodex.core.mention.mentionCandidates
import com.lyrismet.dndcodex.core.mention.mentionEntitiesFrom
import com.lyrismet.dndcodex.core.mention.mentionsIn
import com.lyrismet.dndcodex.domain.model.SessionEntry
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.lyrismet.dndcodex.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.default_campaign_name
import dndplayerscodex.shared.generated.resources.new_session_default_title
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import dndplayerscodex.shared.generated.resources.session_list_live_no_notes
import dndplayerscodex.shared.generated.resources.session_list_live_notes_summary_format
import dndplayerscodex.shared.generated.resources.session_overline_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SessionListPresenter(
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val mentionRepositories: MentionRepositories,
) : Presenter<SessionListState> {
    @Composable
    override fun present(): SessionListState {
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val npcs by mentionRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by mentionRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by mentionRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        // campaign name isn't backed by settings yet - the Settings screen will own this once it exists
        val campaignName = stringResource(Res.string.default_campaign_name)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val noNotesLabel = stringResource(Res.string.session_list_live_no_notes)
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), questPrefix)

        val liveNote = sessions.firstOrNull { it.isLive }
        val liveEntriesFlow: Flow<List<SessionEntry>> =
            remember(liveNote?.id) {
                liveNote?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
            }
        val liveEntries by liveEntriesFlow.collectAsState(initial = emptyList())

        val allItems = sessions.toListItems()
        val liveItem = liveNote?.let { note -> allItems.first { it.id == note.id } }
        val liveSession = liveItem?.let { item -> buildLiveSession(item, liveEntries, candidates, noNotesLabel) }

        val selectedEntityRef = remember { mutableStateOf<EntityRef?>(null) }
        val entitySheet =
            EntitySheetInteractions(
                selectedEntityRef,
                mentionRepositories.npcRepository,
                mentionRepositories.questRepository,
                navigator,
            )
        val selectedEntity =
            selectedEntitySummary(selectedEntityRef.value, npcs, locations, quests, sessions, allEntries, candidates)

        return SessionListState(
            campaignName = campaignName,
            liveSession = liveSession,
            sessions = if (liveNote != null) allItems.filterNot { it.id == liveNote.id } else allItems,
            selectedEntity = selectedEntity,
        ) { event -> onEvent(event, scope, newSessionTitle, entitySheet) }
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
    private fun onEvent(
        event: SessionListEvent,
        scope: CoroutineScope,
        newSessionTitle: String,
        entitySheet: EntitySheetInteractions,
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
            is SessionListEvent.DeleteSessionClicked -> scope.launch { sessionNoteRepository.delete(event.id) }
            is SessionListEvent.MentionChipClicked -> entitySheet.onEntityClicked(event.ref)
            is SessionListEvent.NpcStatusSelected ->
                entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is SessionListEvent.QuestStatusSelected ->
                entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is SessionListEvent.RelatedNoteClicked -> entitySheet.onRelatedNoteClicked(event.sessionNoteId)
            SessionListEvent.SheetDismissed -> entitySheet.onDismissed()
        }
    }

    // numbered by chronological order (oldest = I) even though [this] arrives newest-first from the repo
    private fun List<SessionNote>.toListItems(): List<SessionListItem> {
        val numberLabels = chronologicalNumberLabels()
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
