package com.lyrismet.incadent.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.campaign.CampaignSheetEvent
import com.lyrismet.incadent.core.campaign.CampaignSheetState
import com.lyrismet.incadent.core.campaign.rememberCampaignSheetHost
import com.lyrismet.incadent.core.designsystem.component.toChipItem
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.rememberFullPhoto
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.entitysummary.withFullPhoto
import com.lyrismet.incadent.core.format.chronologicalIndex
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.core.format.russianPluralForm
import com.lyrismet.incadent.core.format.sessionNumberLabels
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.mention.mentionsIn
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.core.swipehint.rememberSwipeHintTarget
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.CampaignStatus
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.model.SwipeHintState
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
import com.lyrismet.incadent.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
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
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val campaignRepository: CampaignRepository,
) : Presenter<SessionListState> {
    // one collection per source the screen reads plus the rename wiring - a flat merge, not real complexity
    @Suppress("LongMethod")
    @Composable
    override fun present(): SessionListState {
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val npcs by mentionRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by mentionRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by mentionRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val parties by partyRepository.observeAll().collectAsState(initial = emptyList())
        val tagsBySession by tagRepository.observeAllSessionTags().collectAsState(initial = emptyMap())
        val scope = rememberCoroutineScope()
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val noNotesLabel = stringResource(Res.string.session_list_live_no_notes)
        val undoDeletedTitle = stringResource(Res.string.session_list_undo_deleted_title)
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests, parties), questPrefix)
        val toast = remember { mutableStateOf<SessionListToast?>(null) }
        toast.autoDismiss()
        val campaignHost =
            rememberCampaignSheetHost(campaignRepository) { text -> toast.value = SessionListToast(text) }
        val campaignArchived = campaignHost.current?.status == CampaignStatus.ARCHIVED
        val entriesBySession = allEntries.groupBy { it.sessionNoteId }

        val liveNote = sessions.firstOrNull { it.isLive }
        val liveEntriesFlow: Flow<List<SessionEntry>> =
            remember(liveNote?.id) {
                liveNote?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
            }
        val liveEntries by liveEntriesFlow.collectAsState(initial = emptyList())

        val allItems =
            remember(sessions, numbering, tagsBySession, entriesBySession, candidates) {
                sessions.toListItems(numbering, tagsBySession, entriesBySession, candidates)
            }
        val liveItem = liveNote?.let { note -> allItems.first { it.id == note.id } }
        val liveSession = liveItem?.let { item -> buildLiveSession(item, liveEntries, candidates, noNotesLabel) }

        val selectedEntityRef = remember { mutableStateOf<EntityRef?>(null) }
        val entityHistory = remember { mutableStateOf(emptyList<EntityRef>()) }
        val entitySheet =
            EntitySheetInteractions(
                selectedEntityRef,
                mentionRepositories,
                partyRepository,
                navigator,
                entityHistory,
            )
        val selectedSummary =
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
        val fullPhoto =
            rememberFullPhoto(
                selectedSummary,
                mentionRepositories.npcRepository,
                partyRepository,
                mentionRepositories.locationRepository,
            )
        val selectedEntity = selectedSummary?.withFullPhoto(fullPhoto)

        val archiveItems = if (liveNote != null) allItems.filterNot { it.id == liveNote.id } else allItems
        val swipeHintState by appPreferencesRepository
            .observeSessionListSwipeHintState()
            .collectAsState(initial = SwipeHintState.PENDING)
        val isTabSelected = LocalTabSelected.current
        val swipeHintTarget =
            rememberSwipeHintTarget(
                hintState = swipeHintState,
                markSeen = { appPreferencesRepository.setSessionListSwipeHintState(SwipeHintState.SEEN) },
                undoController = undoController,
                firstItemId = archiveItems.firstOrNull()?.id,
                // a hidden tab stays composed in the background, so it must not burn its one-time hint unseen
                isBlocked =
                    selectedEntity != null ||
                        campaignHost.sheet != CampaignSheetState.Hidden ||
                        !isTabSelected,
            )

        return SessionListState(
            campaignName = campaignHost.current?.name.orEmpty(),
            campaignArchived = campaignArchived,
            campaignId = campaignHost.current?.id,
            campaignSheet = campaignHost.sheet,
            liveSession = liveSession,
            sessions = archiveItems,
            selectedEntity = selectedEntity,
            toast = toast.value,
            swipeHintTarget = swipeHintTarget,
        ) { event ->
            onEvent(
                event,
                scope,
                newSessionTitle,
                undoDeletedTitle,
                sessions,
                entitySheet,
                numbering,
                campaignHost.onEvent,
                campaignArchived,
            )
        }
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
        onCampaignEvent: (CampaignSheetEvent) -> Unit,
        campaignArchived: Boolean,
    ) {
        when (event) {
            SessionListEvent.NewSessionClicked ->
                if (!campaignArchived) {
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
                }

            is SessionListEvent.SessionClicked -> navigator.goTo(SessionDetailScreen(event.id))
            is SessionListEvent.DeleteSessionClicked ->
                if (!campaignArchived) onDeleteSessionClicked(event.id, sessions, undoDeletedTitle, scope, numbering)
            is SessionListEvent.MentionChipClicked -> entitySheet.onEntityClicked(event.ref)
            is SessionListEvent.NpcStatusSelected ->
                entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is SessionListEvent.NpcLifeSelected ->
                entitySheet.onNpcLifeSelected(scope, event.npcId, event.lifeState)
            is SessionListEvent.QuestStatusSelected ->
                entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is SessionListEvent.RelatedNoteClicked ->
                entitySheet.onRelatedNoteClicked(event.sessionNoteId, event.entryId)
            SessionListEvent.SheetDismissed -> entitySheet.onDismissed()
            is SessionListEvent.CampaignSheet -> onCampaignEvent(event.event)
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
    @Suppress("LongParameterList")
    private fun List<SessionNote>.toListItems(
        numbering: SessionNumbering,
        tagsBySession: Map<Long, List<String>>,
        entriesBySession: Map<Long, List<SessionEntry>>,
        candidates: List<MentionCandidate>,
    ): List<SessionListItem> {
        val numberLabels = sessionNumberLabels(numbering)
        val arabicNumbers = chronologicalIndex()
        return map { note ->
            val entries = entriesBySession[note.id].orEmpty()
            val bodies = entries.map { it.body }
            val dateLabel = note.sessionDate.toDisplayDate()
            val noteCountLabel = "${entries.size} ${russianPluralForm(entries.size, "заметка", "заметки", "заметок")}"
            SessionListItem(
                id = note.id,
                numberLabel = numberLabels.getValue(note.id),
                arabicNumber = arabicNumbers.getValue(note.id),
                title = note.title,
                dateLabel = dateLabel,
                archiveMeta = joinWithDot(dateLabel, noteCountLabel),
                tags = tagsBySession[note.id].orEmpty(),
                mentionCount = mentionsIn(bodies, candidates).size,
            )
        }
    }
}
