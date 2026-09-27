package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.core.designsystem.component.EntityLookup
import com.lyrismet.dndcodex.core.designsystem.component.EntityRef
import com.lyrismet.dndcodex.core.designsystem.component.EntitySummaryItem
import com.lyrismet.dndcodex.core.designsystem.component.buildEntitySummary
import com.lyrismet.dndcodex.core.designsystem.component.toChipItem
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toDisplayTime
import com.lyrismet.dndcodex.core.format.toRomanNumeral
import com.lyrismet.dndcodex.core.mention.mentionCandidates
import com.lyrismet.dndcodex.core.mention.mentionEntitiesFrom
import com.lyrismet.dndcodex.core.mention.mentionsIn
import com.lyrismet.dndcodex.domain.model.Location
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.Quest
import com.lyrismet.dndcodex.domain.model.QuestStatus
import com.lyrismet.dndcodex.domain.model.SessionEntry
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.lyrismet.dndcodex.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_npc_status_dead
import dndplayerscodex.shared.generated.resources.codex_npc_status_enemy
import dndplayerscodex.shared.generated.resources.codex_npc_status_friend
import dndplayerscodex.shared.generated.resources.codex_npc_status_neutral
import dndplayerscodex.shared.generated.resources.codex_quest_status_active
import dndplayerscodex.shared.generated.resources.codex_quest_status_completed
import dndplayerscodex.shared.generated.resources.codex_quest_status_failed
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
        val npcs by mentionRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val locations by mentionRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val quests by mentionRepositories.questRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        // campaign name isn't backed by settings yet - the Settings screen will own this once it exists
        val campaignName = stringResource(Res.string.default_campaign_name)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val noNotesLabel = stringResource(Res.string.session_list_live_no_notes)

        val liveNote = sessions.firstOrNull { it.isLive }
        val liveEntriesFlow: Flow<List<SessionEntry>> =
            remember(liveNote?.id) {
                liveNote?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
            }
        val liveEntries by liveEntriesFlow.collectAsState(initial = emptyList())

        val allItems = sessions.toListItems()
        val liveItem = liveNote?.let { note -> allItems.first { it.id == note.id } }
        val liveSession =
            liveItem?.let { item ->
                buildLiveSession(item, liveEntries, npcs, locations, quests, questPrefix, noNotesLabel)
            }

        val selectedEntityRef = remember { mutableStateOf<EntityRef?>(null) }
        val selectedEntity = selectedEntitySummary(selectedEntityRef.value, npcs, locations, quests)

        return SessionListState(
            campaignName = campaignName,
            liveSession = liveSession,
            sessions = if (liveNote != null) allItems.filterNot { it.id == liveNote.id } else allItems,
            selectedEntity = selectedEntity,
        ) { event -> onEvent(event, scope, newSessionTitle, selectedEntityRef) }
    }

    @Composable
    private fun buildLiveSession(
        item: SessionListItem,
        liveEntries: List<SessionEntry>,
        npcs: List<Npc>,
        locations: List<Location>,
        quests: List<Quest>,
        questPrefix: String,
        noNotesLabel: String,
    ): LiveSessionItem {
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), questPrefix)
        val mentions = mentionsIn(liveEntries.map { it.body }, candidates).take(5).map { it.toChipItem() }
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
            overline = stringResource(Res.string.session_overline_format, item.numberLabel),
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
        selectedEntityRef: MutableState<EntityRef?>,
    ) {
        when (event) {
            SessionListEvent.NewSessionClicked ->
                scope.launch {
                    val id =
                        sessionNoteRepository.upsert(
                            SessionNote(
                                id = 0,
                                title = newSessionTitle,
                                sessionDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                                endedAt = null,
                            ),
                        )
                    navigator.goTo(SessionDetailScreen(id))
                }
            is SessionListEvent.SessionClicked -> navigator.goTo(SessionDetailScreen(event.id))
            is SessionListEvent.DeleteSessionClicked -> scope.launch { sessionNoteRepository.delete(event.id) }
            is SessionListEvent.MentionChipClicked -> selectedEntityRef.value = event.ref
            SessionListEvent.SheetDismissed -> selectedEntityRef.value = null
        }
    }

    // numbered by chronological order (oldest = I) even though [this] arrives newest-first from the repo
    private fun List<SessionNote>.toListItems(): List<SessionListItem> =
        mapIndexed { index, note ->
            SessionListItem(
                id = note.id,
                numberLabel = (size - index).toRomanNumeral(),
                title = note.title,
                dateLabel = note.sessionDate.toDisplayDate(),
            )
        }
}

@Composable
private fun selectedEntitySummary(
    ref: EntityRef?,
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
): EntitySummaryItem? {
    if (ref == null) return null
    val npcStatusLabels =
        mapOf(
            NpcStatus.FRIEND to stringResource(Res.string.codex_npc_status_friend),
            NpcStatus.ENEMY to stringResource(Res.string.codex_npc_status_enemy),
            NpcStatus.NEUTRAL to stringResource(Res.string.codex_npc_status_neutral),
            NpcStatus.DEAD to stringResource(Res.string.codex_npc_status_dead),
        )
    val questStatusLabels =
        mapOf(
            QuestStatus.ACTIVE to stringResource(Res.string.codex_quest_status_active),
            QuestStatus.COMPLETED to stringResource(Res.string.codex_quest_status_completed),
            QuestStatus.FAILED to stringResource(Res.string.codex_quest_status_failed),
        )
    return buildEntitySummary(ref, EntityLookup(npcs, locations, quests, npcStatusLabels, questStatusLabels))
}
