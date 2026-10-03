package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.npcStatusLabels
import com.lyrismet.incadent.core.entitysummary.questStatusLabels
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_type_location
import dndplayerscodex.shared.generated.resources.codex_entry_type_npc
import dndplayerscodex.shared.generated.resources.codex_entry_type_quest
import dndplayerscodex.shared.generated.resources.codex_filter_all
import dndplayerscodex.shared.generated.resources.codex_undo_deleted_title
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
private class CodexFields(
    val activeTab: MutableState<CodexTab>,
    val searchQuery: MutableState<String>,
    val npcStatusFilter: MutableState<NpcStatus?>,
    val questStatusFilter: MutableState<QuestStatus?>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
)

/** result of applying the search query (and, for the two lists that have one, the status filter) */
private data class CodexSearchResults(
    val npcsById: Map<Long, Npc>,
    val searchedNpcs: List<Npc>,
    val searchedQuests: List<Quest>,
    val searchedLocations: List<Location>,
    val filteredNpcs: List<Npc>,
    val filteredQuests: List<Quest>,
)

@Suppress("LongParameterList")
class CodexPresenter(
    private val navigator: Navigator,
    private val npcRepository: NpcRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter<CodexState> {
    @Composable
    override fun present(): CodexState {
        val npcs by npcRepository.observeAll().collectAsState(initial = emptyList())
        val quests by questRepository.observeAll().collectAsState(initial = emptyList())
        val locations by locationRepository.observeAll().collectAsState(initial = emptyList())
        val sessionNotes by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val sessionEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val scope = rememberCoroutineScope()

        // rememberRetained not remember - state must survive push/pop navigation, not just recomposition
        val activeTab = rememberRetained { mutableStateOf(CodexTab.ALL) }
        val searchQuery = rememberRetained { mutableStateOf("") }
        val npcStatusFilter = rememberRetained { mutableStateOf<NpcStatus?>(null) }
        val questStatusFilter = rememberRetained { mutableStateOf<QuestStatus?>(null) }
        val selectedEntityRef = rememberRetained { mutableStateOf<EntityRef?>(null) }
        val entitySheet = EntitySheetInteractions(selectedEntityRef, npcRepository, questRepository, navigator)
        val formController =
            CodexEntryFormController.rememberController(npcRepository, questRepository, locationRepository)
        val fields =
            CodexFields(activeTab, searchQuery, npcStatusFilter, questStatusFilter, selectedEntityRef, entitySheet)

        val allLabel = stringResource(Res.string.codex_filter_all)
        val npcStatusLabels = npcStatusLabels()
        val questStatusLabels = questStatusLabels()
        val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), questPrefix)
        val undoDeletedTitle = stringResource(Res.string.codex_undo_deleted_title)
        val entityTypeLabels =
            mapOf(
                CodexEntryType.NPC to stringResource(Res.string.codex_entry_type_npc),
                CodexEntryType.QUEST to stringResource(Res.string.codex_entry_type_quest),
                CodexEntryType.LOCATION to stringResource(Res.string.codex_entry_type_location),
            )

        val search = buildSearchResults(npcs, quests, locations, fields)

        val selectedEntity =
            selectedEntitySummary(
                selectedEntityRef.value,
                npcs,
                locations,
                quests,
                sessionNotes,
                sessionEntries,
                candidates,
                numbering,
            )

        return buildCodexState(
            fields,
            formController,
            npcs,
            quests,
            locations,
            search,
            selectedEntity,
            npcStatusLabels,
            questStatusLabels,
            allLabel,
            undoDeletedTitle,
            entityTypeLabels,
            scope,
        )
    }

    // assembles the full screen state from already-computed pieces - a flat data merge, not real complexity
    @Suppress("LongParameterList")
    @Composable
    private fun buildCodexState(
        fields: CodexFields,
        formController: CodexEntryFormController,
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        search: CodexSearchResults,
        selectedEntity: EntitySummaryItem?,
        npcStatusLabels: Map<NpcStatus, String>,
        questStatusLabels: Map<QuestStatus, String>,
        allLabel: String,
        undoDeletedTitle: String,
        entityTypeLabels: Map<CodexEntryType, String>,
        scope: CoroutineScope,
    ): CodexState =
        CodexState(
            activeTab = fields.activeTab.value,
            searchQuery = fields.searchQuery.value,
            tabCounts = CodexTabCounts(party = 0, npc = npcs.size, quest = quests.size, location = locations.size),
            npcs = search.filteredNpcs.toCodexItems(npcStatusLabels),
            npcStatusFilter = fields.npcStatusFilter.value,
            npcFilterOptions =
                npcFilterOptions(search.searchedNpcs.groupingBy { it.status }.eachCount(), npcStatusLabels, allLabel),
            quests = search.filteredQuests.toCodexItems(search.npcsById, questStatusLabels),
            questStatusFilter = fields.questStatusFilter.value,
            questFilterOptions =
                questFilterOptions(
                    search.searchedQuests.groupingBy { it.status }.eachCount(),
                    questStatusLabels,
                    allLabel,
                ),
            locations = search.searchedLocations.toCodexItems(),
            activeSheet = codexActiveSheet(formController.buildState(npcs, locations), selectedEntity),
        ) { event ->
            onEvent(event, fields, formController, npcs, quests, locations, undoDeletedTitle, entityTypeLabels, scope)
        }

    private fun buildSearchResults(
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        fields: CodexFields,
    ): CodexSearchResults {
        val query = fields.searchQuery.value.trim()
        val npcsById = npcs.associateBy { it.id }
        val searchedNpcs = npcs.filter { it.matchesQuery(query) }
        val searchedQuests = quests.filter { it.matchesQuery(query, npcsById) }
        val npcStatusFilter = fields.npcStatusFilter.value
        val questStatusFilter = fields.questStatusFilter.value
        return CodexSearchResults(
            npcsById = npcsById,
            searchedNpcs = searchedNpcs,
            searchedQuests = searchedQuests,
            searchedLocations = locations.filter { it.matchesQuery(query) },
            filteredNpcs = searchedNpcs.filter { npcStatusFilter == null || it.status == npcStatusFilter },
            filteredQuests = searchedQuests.filter { questStatusFilter == null || it.status == questStatusFilter },
        )
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

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongParameterList")
    private fun onEvent(
        event: CodexEvent,
        fields: CodexFields,
        formController: CodexEntryFormController,
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        undoDeletedTitle: String,
        entityTypeLabels: Map<CodexEntryType, String>,
        scope: CoroutineScope,
    ) {
        when (event) {
            is CodexEvent.TabSelected -> fields.activeTab.value = event.tab
            is CodexEvent.SearchQueryChanged -> fields.searchQuery.value = event.query
            is CodexEvent.NpcStatusFilterSelected -> fields.npcStatusFilter.value = event.status
            is CodexEvent.QuestStatusFilterSelected -> fields.questStatusFilter.value = event.status
            is CodexEvent.EntityClicked -> fields.entitySheet.onEntityClicked(event.ref)
            is CodexEvent.NpcStatusSelected -> fields.entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is CodexEvent.QuestStatusSelected ->
                fields.entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is CodexEvent.RelatedNoteClicked -> fields.entitySheet.onRelatedNoteClicked(event.sessionNoteId)
            CodexEvent.SheetDismissed -> fields.entitySheet.onDismissed()
            CodexEvent.AddEntryClicked -> formController.onAddEntryClicked(fields.activeTab.value.toEntryType())
            is CodexEvent.EditEntryRequested -> formController.onEditEntryRequested(event.ref, scope)
            is CodexEvent.EntryTypeChanged -> formController.onTypeChanged(event.type)
            is CodexEvent.EntryFieldChanged -> formController.onFieldChanged(event.field, event.text)
            is CodexEvent.EntryNpcStatusChanged -> formController.onNpcStatusChanged(event.status)
            is CodexEvent.EntryQuestStatusChanged -> formController.onQuestStatusChanged(event.status)
            is CodexEvent.EntryChipToggled -> formController.onChipToggled(event.field, event.id)
            CodexEvent.EntryFormSaveClicked -> formController.onSaveClicked(scope)
            CodexEvent.EntryFormClosed -> formController.onClosed()
            is CodexEvent.EntityDeleteRequested ->
                onEntityDeleteRequested(event.ref, npcs, quests, locations, undoDeletedTitle, entityTypeLabels, scope)
        }
    }

    // deletes immediately and offers undo - restoring also re-links whatever else referenced the deleted entity
    @Suppress("LongParameterList")
    private fun onEntityDeleteRequested(
        ref: EntityRef,
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        undoDeletedTitle: String,
        entityTypeLabels: Map<CodexEntryType, String>,
        scope: CoroutineScope,
    ) {
        val plan = codexDeletionPlan(ref, npcs, quests, locations, entityTypeLabels) ?: return
        scope.launch {
            plan.delete()
            undoController.show(undoDeletedTitle, plan.subtitle, plan.restore)
        }
    }

    @Suppress("LongParameterList")
    private fun codexDeletionPlan(
        ref: EntityRef,
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        entityTypeLabels: Map<CodexEntryType, String>,
    ): CodexDeletionPlan? =
        when (ref) {
            is EntityRef.Npc ->
                npcs.find { it.id == ref.id }?.let { npc ->
                    val givenQuestIds = quests.filter { it.givenByNpcId == ref.id }.map { it.id }
                    CodexDeletionPlan(
                        subtitle = "${entityTypeLabels.getValue(CodexEntryType.NPC)} · ${npc.name}",
                        delete = { npcRepository.delete(ref.id) },
                        restore = {
                            val newId = npcRepository.upsert(npc.copy(id = 0))
                            givenQuestIds.forEach { id ->
                                questRepository.getById(id)?.let {
                                    questRepository.upsert(it.copy(givenByNpcId = newId))
                                }
                            }
                        },
                    )
                }

            is EntityRef.Quest ->
                quests.find { it.id == ref.id }?.let { quest ->
                    CodexDeletionPlan(
                        subtitle = "${entityTypeLabels.getValue(CodexEntryType.QUEST)} · ${quest.title}",
                        delete = { questRepository.delete(ref.id) },
                        restore = { questRepository.upsert(quest.copy(id = 0)) },
                    )
                }

            is EntityRef.Location ->
                locations.find { it.id == ref.id }?.let { location ->
                    val npcIdsHere = npcs.filter { it.locationId == ref.id }.map { it.id }
                    val questIdsHere = quests.filter { it.locationId == ref.id }.map { it.id }
                    CodexDeletionPlan(
                        subtitle = "${entityTypeLabels.getValue(CodexEntryType.LOCATION)} · ${location.name}",
                        delete = { locationRepository.delete(ref.id) },
                        restore = {
                            val newId = locationRepository.upsert(location.copy(id = 0))
                            npcIdsHere.forEach { id ->
                                npcRepository.getById(id)?.let { npcRepository.upsert(it.copy(locationId = newId)) }
                            }
                            questIdsHere.forEach { id ->
                                questRepository.getById(id)?.let {
                                    questRepository.upsert(it.copy(locationId = newId))
                                }
                            }
                        },
                    )
                }
        }
}

private class CodexDeletionPlan(
    val subtitle: String,
    val delete: suspend () -> Unit,
    val restore: suspend () -> Unit,
)

// the form always wins so editing never leaves the entity-view sheet stacked underneath it
private fun codexActiveSheet(
    formState: CodexEntryFormState?,
    selectedEntity: EntitySummaryItem?,
): CodexSheet? =
    when {
        formState != null -> CodexSheet.EntryForm(formState)
        selectedEntity != null -> CodexSheet.EntityView(selectedEntity)
        else -> null
    }

// mirrors the mockup's openNew(): "Все" (and the unsupported "Отряд") fall back to NPC, every other tab keeps its type
private fun CodexTab.toEntryType(): CodexEntryType =
    when (this) {
        CodexTab.QUEST -> CodexEntryType.QUEST
        CodexTab.LOCATION -> CodexEntryType.LOCATION
        CodexTab.ALL, CodexTab.PARTY, CodexTab.NPC -> CodexEntryType.NPC
    }
