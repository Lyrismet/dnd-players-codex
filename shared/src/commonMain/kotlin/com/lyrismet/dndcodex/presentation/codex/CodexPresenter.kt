package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.dndcodex.core.designsystem.component.EntityLookup
import com.lyrismet.dndcodex.core.designsystem.component.EntityRef
import com.lyrismet.dndcodex.core.designsystem.component.buildEntitySummary
import com.lyrismet.dndcodex.core.designsystem.toStatusColor
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus
import com.lyrismet.dndcodex.domain.repository.LocationRepository
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.lyrismet.dndcodex.domain.repository.QuestRepository
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_filter_all
import dndplayerscodex.shared.generated.resources.codex_npc_status_dead
import dndplayerscodex.shared.generated.resources.codex_npc_status_enemy
import dndplayerscodex.shared.generated.resources.codex_npc_status_friend
import dndplayerscodex.shared.generated.resources.codex_npc_status_neutral
import dndplayerscodex.shared.generated.resources.codex_quest_status_active
import dndplayerscodex.shared.generated.resources.codex_quest_status_completed
import dndplayerscodex.shared.generated.resources.codex_quest_status_failed
import org.jetbrains.compose.resources.stringResource

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
private class CodexFields(
    val activeTab: MutableState<CodexTab>,
    val searchQuery: MutableState<String>,
    val npcStatusFilter: MutableState<NpcStatus?>,
    val questStatusFilter: MutableState<QuestStatus?>,
    val selectedEntityRef: MutableState<EntityRef?>,
)

class CodexPresenter(
    private val npcRepository: NpcRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) : Presenter<CodexState> {
    @Composable
    override fun present(): CodexState {
        val npcs by npcRepository.observeAll().collectAsState(initial = emptyList())
        val quests by questRepository.observeAll().collectAsState(initial = emptyList())
        val locations by locationRepository.observeAll().collectAsState(initial = emptyList())

        // rememberRetained not remember - state must survive push/pop navigation, not just recomposition
        val activeTab = rememberRetained { mutableStateOf(CodexTab.ALL) }
        val searchQuery = rememberRetained { mutableStateOf("") }
        val npcStatusFilter = rememberRetained { mutableStateOf<NpcStatus?>(null) }
        val questStatusFilter = rememberRetained { mutableStateOf<QuestStatus?>(null) }
        val selectedEntityRef = rememberRetained { mutableStateOf<EntityRef?>(null) }
        val fields = CodexFields(activeTab, searchQuery, npcStatusFilter, questStatusFilter, selectedEntityRef)

        val allLabel = stringResource(Res.string.codex_filter_all)
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

        val query = searchQuery.value.trim()
        val npcsById = npcs.associateBy { it.id }
        val searchedNpcs = npcs.filter { it.matchesQuery(query) }
        val searchedQuests = quests.filter { it.matchesQuery(query, npcsById) }
        val searchedLocations = locations.filter { it.matchesQuery(query) }
        val filteredNpcs = searchedNpcs.filter { npcStatusFilter.value == null || it.status == npcStatusFilter.value }
        val filteredQuests =
            searchedQuests.filter { questStatusFilter.value == null || it.status == questStatusFilter.value }

        val selectedEntity =
            selectedEntityRef.value?.let { ref ->
                buildEntitySummary(ref, EntityLookup(npcs, locations, quests, npcStatusLabels, questStatusLabels))
            }

        return CodexState(
            activeTab = activeTab.value,
            searchQuery = searchQuery.value,
            tabCounts = CodexTabCounts(party = 0, npc = npcs.size, quest = quests.size, location = locations.size),
            npcs = filteredNpcs.toCodexItems(npcStatusLabels),
            npcStatusFilter = npcStatusFilter.value,
            npcFilterOptions =
                npcFilterOptions(searchedNpcs.groupingBy { it.status }.eachCount(), npcStatusLabels, allLabel),
            quests = filteredQuests.toCodexItems(npcsById, questStatusLabels),
            questStatusFilter = questStatusFilter.value,
            questFilterOptions =
                questFilterOptions(searchedQuests.groupingBy { it.status }.eachCount(), questStatusLabels, allLabel),
            locations = searchedLocations.toCodexItems(),
            locationFilterOptions =
                listOf(
                    CodexFilterOption(value = null, label = allLabel, count = searchedLocations.size, dotColor = null),
                ),
            selectedEntity = selectedEntity,
        ) { event -> onEvent(event, fields) }
    }

    private fun npcFilterOptions(
        counts: Map<NpcStatus, Int>,
        labels: Map<NpcStatus, String>,
        allLabel: String,
    ): List<CodexFilterOption<NpcStatus>> =
        listOf<CodexFilterOption<NpcStatus>>(
            CodexFilterOption(value = null, label = allLabel, count = counts.values.sum(), dotColor = null),
        ) +
            NpcStatus.entries.map { status ->
                CodexFilterOption(
                    value = status,
                    label = labels.getValue(status),
                    count = counts[status] ?: 0,
                    dotColor = status.toStatusColor().foreground,
                )
            }

    private fun questFilterOptions(
        counts: Map<QuestStatus, Int>,
        labels: Map<QuestStatus, String>,
        allLabel: String,
    ): List<CodexFilterOption<QuestStatus>> =
        listOf<CodexFilterOption<QuestStatus>>(
            CodexFilterOption(value = null, label = allLabel, count = counts.values.sum(), dotColor = null),
        ) +
            QuestStatus.entries.map { status ->
                CodexFilterOption(
                    value = status,
                    label = labels.getValue(status),
                    count = counts[status] ?: 0,
                    dotColor = status.toStatusColor().foreground,
                )
            }

    private fun onEvent(
        event: CodexEvent,
        fields: CodexFields,
    ) {
        when (event) {
            is CodexEvent.TabSelected -> fields.activeTab.value = event.tab
            is CodexEvent.SearchQueryChanged -> fields.searchQuery.value = event.query
            is CodexEvent.NpcStatusFilterSelected -> fields.npcStatusFilter.value = event.status
            is CodexEvent.QuestStatusFilterSelected -> fields.questStatusFilter.value = event.status
            is CodexEvent.EntityClicked -> fields.selectedEntityRef.value = event.ref
            CodexEvent.SheetDismissed -> fields.selectedEntityRef.value = null
            // creation form is out of scope for now (FEATURES.md section 6) - the header button stays inert
            CodexEvent.AddEntryClicked -> Unit
        }
    }
}
