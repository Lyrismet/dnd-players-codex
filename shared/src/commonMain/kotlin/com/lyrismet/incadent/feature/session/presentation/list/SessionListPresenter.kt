package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.format.toDisplayDate
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.HoldHintState
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.preferences.SwipeHintState
import com.lyrismet.incadent.core.swipehint.rememberSwipeHintTarget
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.lyrismet.incadent.feature.campaign.presentation.sheet.rememberCampaignSheetHost
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditInteractions
import com.lyrismet.incadent.feature.codex.presentation.quickedit.rememberEntityChangeReporter
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.isEntityLoading
import com.lyrismet.incadent.feature.codex.presentation.summary.onEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.rememberFullPhoto
import com.lyrismet.incadent.feature.codex.presentation.summary.selectedEntitySummary
import com.lyrismet.incadent.feature.codex.presentation.summary.withFullPhoto
import com.lyrismet.incadent.feature.codex.presentation.summary.writesCampaignData
import com.lyrismet.incadent.feature.combat.domain.repository.CombatFactsSource
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.mentionsIn
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.session.domain.chronologicalIndex
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import com.lyrismet.incadent.feature.session.presentation.route.SessionDetailScreen
import com.lyrismet.incadent.feature.session.presentation.route.sessionEntryOpener
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.error_action_failed
import dndplayerscodex.shared.generated.resources.new_session_default_title
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
    private val combatFactsSource: CombatFactsSource,
    private val codexRepositories: CodexRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val campaignRepository: CampaignRepository,
    private val importSource: ImportSource,
    private val imageCompressor: ImageCompressor,
) : Presenter<SessionListState> {
    // one collection per source the screen reads plus the rename wiring - a flat merge, not real complexity
    @Suppress("LongMethod")
    @Composable
    override fun present(): SessionListState {
        val loadedSessions by sessionNoteRepository.observeAll().collectAsState(initial = null)
        val sessions = loadedSessions.orEmpty()
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val npcs by codexRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by codexRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by codexRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val parties by codexRepositories.partyRepository.observeAll().collectAsState(initial = emptyList())
        val species by codexRepositories.speciesRepository.observeAll().collectAsState(initial = emptyList())
        val facts by combatFactsSource.observe().collectAsState(initial = CombatFacts.EMPTY)
        val tagsBySession by tagRepository.observeAllSessionTags().collectAsState(initial = emptyMap())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)
        val noNotesLabel = stringResource(Res.string.session_list_live_no_notes)
        val undoDeletedTitle = stringResource(Res.string.session_list_undo_deleted_title)
        val candidates =
            remember(npcs, locations, quests, parties, species) {
                MentionIndex(mentionEntitiesFrom(npcs, locations, quests, parties, species))
            }
        val toast = remember { mutableStateOf<ToastMessage?>(null) }
        toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val scope = rememberGuardedScope { toast.value = ToastMessage(failedText) }
        val campaignHost =
            rememberCampaignSheetHost(campaignRepository, importSource) { text -> toast.value = ToastMessage(text) }
        val campaignArchived = campaignHost.current?.status == CampaignStatus.ARCHIVED
        val entriesBySession = remember(allEntries) { allEntries.groupBy { it.sessionNoteId } }

        val liveNote = sessions.firstOrNull { it.isLive }
        val liveEntriesFlow: Flow<List<SessionEntry>> =
            remember(liveNote?.id) {
                liveNote?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
            }
        val liveEntries by liveEntriesFlow.collectAsState(initial = emptyList())

        val language = currentAppLanguage()
        // a session whose notes did not change keeps its mention count, so a saved note re-parses only its own session
        val mentionCounts = remember(candidates) { HashMap<Long, Pair<List<String>, Int>>() }
        val allItems =
            remember(sessions, numbering, tagsBySession, entriesBySession, candidates, language) {
                sessions.toListItems(numbering, tagsBySession, entriesBySession, language) { id, bodies ->
                    mentionCounts[id]?.takeIf { it.first == bodies }?.second
                        ?: mentionsIn(bodies, candidates).size.also { mentionCounts[id] = bodies to it }
                }
            }
        val liveItem = liveNote?.let { note -> allItems.first { it.id == note.id } }
        val liveSession = liveItem?.let { item -> buildLiveSession(item, liveEntries, candidates, noNotesLabel) }

        val selectedEntityRef = remember { mutableStateOf<EntityRef?>(null) }
        val entityHistory = remember { mutableStateOf(emptyList<EntityRef>()) }
        val entitySheet =
            EntitySheetInteractions(
                selectedEntityRef,
                codexRepositories,
                navigator.sessionEntryOpener(),
                entityHistory,
                onChanged = rememberEntityChangeReporter(undoController),
            )
        val quickEdit =
            QuickEditInteractions(
                remember { mutableStateOf(null) },
                remember { mutableStateOf(null) },
                selectedEntityRef,
                entitySheet,
                imageCompressor,
                remember { mutableStateOf(emptyList()) },
                codexRepositories,
                undoController,
            ) {
                appPreferencesRepository.setHoldHintState(HoldHintState.SEEN)
            }
        val holdHint by appPreferencesRepository.observeHoldHintState().collectAsState(initial = HoldHintState.SEEN)
        val undoAction by undoController.current.collectAsState()
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
                species,
                facts,
            )
        val fullPhoto =
            rememberFullPhoto(selectedSummary, codexRepositories)
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
            isLoading = loadedSessions == null,
            campaignName = campaignHost.current?.name.orEmpty(),
            campaignArchived = campaignArchived,
            campaignId = campaignHost.current?.id,
            campaignSheet = campaignHost.sheet,
            liveSession = liveSession,
            sessions = archiveItems,
            selectedEntity = selectedEntity,
            entityLoading = isEntityLoading(selectedEntityRef.value, selectedEntity),
            quickEdit =
                quickEdit.state(
                    CodexRecords(
                        parties = parties,
                        npcs = npcs,
                        quests = quests,
                        locations = locations,
                        species = species,
                    ),
                    holdTipAllowed = holdHint == HoldHintState.PENDING,
                    undoAction = undoAction,
                ),
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
                quickEdit,
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
        candidates: MentionIndex,
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
        quickEdit: QuickEditInteractions,
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
                        sessionNoteRepository.makeOnlyLive(id, now)
                        navigator.goTo(SessionDetailScreen(id))
                    }
                }

            is SessionListEvent.SessionClicked -> navigator.goTo(SessionDetailScreen(event.id))
            is SessionListEvent.DeleteSessionClicked ->
                if (!campaignArchived) onDeleteSessionClicked(event.id, sessions, undoDeletedTitle, scope, numbering)
            is SessionListEvent.EntitySheet -> {
                if (event.event is EntitySheetEvent.EntityClicked || event.event == EntitySheetEvent.Dismissed) {
                    quickEdit.onSheetClosed()
                }
                if (!campaignArchived || !event.event.writesCampaignData()) entitySheet.onEvent(scope, event.event)
            }
            is SessionListEvent.QuickEdit -> if (!campaignArchived) quickEdit.onEvent(scope, event.event)
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
            val tags = tagRepository.observeTagsFor(id).first()
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
                tags.forEach { tag -> tagRepository.addTag(newId, tag) }
            }
        }
    }

    // numbered by chronological order (oldest = I) even though [this] arrives newest-first from the repo
    @Suppress("LongParameterList")
    private fun List<SessionNote>.toListItems(
        numbering: SessionNumbering,
        tagsBySession: Map<Long, List<String>>,
        entriesBySession: Map<Long, List<SessionEntry>>,
        language: AppLanguage,
        mentionCountOf: (sessionId: Long, bodies: List<String>) -> Int,
    ): List<SessionListItem> {
        val numberLabels = sessionNumberLabels(numbering)
        val arabicNumbers = chronologicalIndex()
        return map { note ->
            val entries = entriesBySession[note.id].orEmpty()
            val bodies = entries.map { it.body }
            val dateLabel = note.sessionDate.toDisplayDate(language)
            SessionListItem(
                id = note.id,
                numberLabel = numberLabels.getValue(note.id),
                arabicNumber = arabicNumbers.getValue(note.id),
                title = note.title,
                dateLabel = dateLabel,
                noteCount = entries.size,
                tags = tagsBySession[note.id].orEmpty(),
                mentionCount = mentionCountOf(note.id, bodies),
            )
        }
    }
}
