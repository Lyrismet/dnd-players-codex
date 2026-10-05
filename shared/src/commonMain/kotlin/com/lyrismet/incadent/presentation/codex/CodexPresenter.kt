package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.codexgroup.CodexGroupLabels
import com.lyrismet.incadent.core.codexgroup.CodexGroupingSelection
import com.lyrismet.incadent.core.codexgroup.LocationGroupBy
import com.lyrismet.incadent.core.codexgroup.NpcGroupBy
import com.lyrismet.incadent.core.codexgroup.QuestGroupBy
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.npcLifeLabels
import com.lyrismet.incadent.core.entitysummary.npcStatusLabels
import com.lyrismet.incadent.core.entitysummary.partyPresenceLabels
import com.lyrismet.incadent.core.entitysummary.questStatusLabels
import com.lyrismet.incadent.core.entitysummary.selectedEntitySummary
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_type_location
import dndplayerscodex.shared.generated.resources.codex_entry_type_npc
import dndplayerscodex.shared.generated.resources.codex_entry_type_party
import dndplayerscodex.shared.generated.resources.codex_entry_type_quest
import dndplayerscodex.shared.generated.resources.codex_group_faction
import dndplayerscodex.shared.generated.resources.codex_group_giver
import dndplayerscodex.shared.generated.resources.codex_group_giver_prefix
import dndplayerscodex.shared.generated.resources.codex_group_no_faction
import dndplayerscodex.shared.generated.resources.codex_group_no_giver
import dndplayerscodex.shared.generated.resources.codex_group_no_place
import dndplayerscodex.shared.generated.resources.codex_group_no_region_quest
import dndplayerscodex.shared.generated.resources.codex_group_no_type
import dndplayerscodex.shared.generated.resources.codex_group_none
import dndplayerscodex.shared.generated.resources.codex_group_region
import dndplayerscodex.shared.generated.resources.codex_group_relation
import dndplayerscodex.shared.generated.resources.codex_group_status
import dndplayerscodex.shared.generated.resources.codex_group_type
import dndplayerscodex.shared.generated.resources.codex_party_card_ac
import dndplayerscodex.shared.generated.resources.codex_party_card_hp
import dndplayerscodex.shared.generated.resources.codex_party_card_player_prefix
import dndplayerscodex.shared.generated.resources.codex_party_card_you
import dndplayerscodex.shared.generated.resources.codex_undo_deleted_title
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
private class CodexFields(
    val activeTab: MutableState<CodexTab>,
    val searchQuery: MutableState<String>,
    val grouping: MutableState<CodexGroupingSelection>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
)

/** result of applying the text search query to every list */
internal data class CodexSearchResults(
    val npcsById: Map<Long, Npc>,
    val searchedParties: List<PartyMember>,
    val searchedNpcs: List<Npc>,
    val searchedQuests: List<Quest>,
    val searchedLocations: List<Location>,
)

/** the labels the codex screen resolves from resources, collected once per composition */
private class CodexLabels(
    val partyCard: PartyCardLabels,
    val entityType: Map<CodexEntryType, String>,
    val undoDeletedTitle: String,
    val grouped: CodexGroupedLabels,
)

@Suppress("LongParameterList")
class CodexPresenter(
    private val navigator: Navigator,
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter<CodexState> {
    private val deletionPlanner =
        CodexDeletionPlanner(npcRepository, partyRepository, questRepository, locationRepository)

    @Composable
    override fun present(): CodexState {
        val parties by partyRepository.observeAll().collectAsState(initial = emptyList())
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
        val grouping = rememberRetained { mutableStateOf(CodexGroupingSelection()) }
        val selectedEntityRef = rememberRetained { mutableStateOf<EntityRef?>(null) }
        val entitySheet =
            EntitySheetInteractions(selectedEntityRef, npcRepository, questRepository, partyRepository, navigator)
        val formController =
            CodexEntryFormController.rememberController(
                npcRepository,
                partyRepository,
                questRepository,
                locationRepository,
            )
        val fields =
            CodexFields(
                activeTab,
                searchQuery,
                grouping,
                selectedEntityRef,
                entitySheet,
            )

        val labels = codexLabels()
        val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), questPrefix())
        val records = CodexRecords(parties, npcs, quests, locations)
        val search = buildSearchResults(records, fields)
        val grouped =
            codexGroupedLists(search, records, activeTab.value, grouping.value, labels.grouped)

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
                parties,
            )

        return buildCodexState(fields, formController, records, search, grouped, selectedEntity, labels, scope)
    }

    @Composable
    private fun codexLabels(): CodexLabels {
        val partyPresence = partyPresenceLabels()
        return CodexLabels(
            partyCard =
                PartyCardLabels(
                    presence = partyPresence,
                    you = stringResource(Res.string.codex_party_card_you),
                    playerPrefix = stringResource(Res.string.codex_party_card_player_prefix),
                    armorClassShort = stringResource(Res.string.codex_party_card_ac),
                    hpShort = stringResource(Res.string.codex_party_card_hp),
                ),
            entityType =
                mapOf(
                    CodexEntryType.PARTY to stringResource(Res.string.codex_entry_type_party),
                    CodexEntryType.NPC to stringResource(Res.string.codex_entry_type_npc),
                    CodexEntryType.QUEST to stringResource(Res.string.codex_entry_type_quest),
                    CodexEntryType.LOCATION to stringResource(Res.string.codex_entry_type_location),
                ),
            undoDeletedTitle = stringResource(Res.string.codex_undo_deleted_title),
            grouped = groupedLabels(npcStatusLabels(), questStatusLabels()),
        )
    }

    @Composable
    private fun groupedLabels(
        npcStatus: Map<NpcStatus, String>,
        questStatus: Map<QuestStatus, String>,
    ): CodexGroupedLabels =
        CodexGroupedLabels(
            titles =
                CodexGroupLabels(
                    noRegionForQuest = stringResource(Res.string.codex_group_no_region_quest),
                    noPlace = stringResource(Res.string.codex_group_no_place),
                    noFaction = stringResource(Res.string.codex_group_no_faction),
                    noGiver = stringResource(Res.string.codex_group_no_giver),
                    giverPrefix = stringResource(Res.string.codex_group_giver_prefix),
                    noType = stringResource(Res.string.codex_group_no_type),
                    npcStatus = npcStatus,
                    questStatus = questStatus,
                ),
            options =
                CodexGroupOptionLabels(
                    npc =
                        mapOf(
                            NpcGroupBy.REGION to stringResource(Res.string.codex_group_region),
                            NpcGroupBy.RELATION to stringResource(Res.string.codex_group_relation),
                            NpcGroupBy.FACTION to stringResource(Res.string.codex_group_faction),
                            NpcGroupBy.NONE to stringResource(Res.string.codex_group_none),
                        ),
                    quest =
                        mapOf(
                            QuestGroupBy.REGION to stringResource(Res.string.codex_group_region),
                            QuestGroupBy.STATUS to stringResource(Res.string.codex_group_status),
                            QuestGroupBy.GIVER to stringResource(Res.string.codex_group_giver),
                            QuestGroupBy.NONE to stringResource(Res.string.codex_group_none),
                        ),
                    place =
                        mapOf(
                            LocationGroupBy.REGION to stringResource(Res.string.codex_group_region),
                            LocationGroupBy.TYPE to stringResource(Res.string.codex_group_type),
                            LocationGroupBy.NONE to stringResource(Res.string.codex_group_none),
                        ),
                ),
            npcLife = npcLifeLabels(),
        )

    @Composable
    private fun questPrefix(): String = stringResource(Res.string.session_detail_quest_mention_prefix)

    // assembles the full screen state from already-computed pieces - a flat data merge, not real complexity
    @Suppress("LongParameterList")
    @Composable
    private fun buildCodexState(
        fields: CodexFields,
        formController: CodexEntryFormController,
        records: CodexRecords,
        search: CodexSearchResults,
        grouped: CodexGroupedLists,
        selectedEntity: EntitySummaryItem?,
        labels: CodexLabels,
        scope: CoroutineScope,
    ): CodexState =
        CodexState(
            activeTab = fields.activeTab.value,
            searchQuery = fields.searchQuery.value,
            tabCounts =
                CodexTabCounts(
                    party = records.parties.size,
                    npc = records.npcs.size,
                    quest = records.quests.size,
                    location = records.locations.size,
                ),
            party = search.searchedParties.toPartyCodexItems(labels.partyCard),
            npcs = grouped.npcs,
            quests = grouped.quests,
            locations = grouped.locations,
            groupBy = grouped.groupBy,
            groupOptions = grouped.groupOptions,
            emptyState =
                codexEmptyState(
                    codexEmptyKind(
                        fields.activeTab.value,
                        fields.searchQuery.value,
                        search.hasEntriesFor(fields.activeTab.value),
                    ),
                ),
            activeSheet = codexActiveSheet(formController.buildState(records.npcs, records.locations), selectedEntity),
        ) { event ->
            onEvent(event, fields, formController, records, labels, scope)
        }

    private fun buildSearchResults(
        records: CodexRecords,
        fields: CodexFields,
    ): CodexSearchResults {
        val query = fields.searchQuery.value.trim()
        val npcsById = records.npcs.associateBy { it.id }
        return CodexSearchResults(
            npcsById = npcsById,
            searchedParties = records.parties.filter { it.matchesQuery(query) },
            searchedNpcs = records.npcs.filter { it.matchesQuery(query, records.locations) },
            searchedQuests = records.quests.filter { it.matchesQuery(query, records.npcs, records.locations) },
            searchedLocations = records.locations.filter { it.matchesQuery(query, records.locations) },
        )
    }

    private fun CodexSearchResults.hasEntriesFor(tab: CodexTab): Boolean =
        when (tab) {
            CodexTab.ALL -> hasAnyEntry()
            CodexTab.PARTY -> searchedParties.isNotEmpty()
            CodexTab.NPC -> searchedNpcs.isNotEmpty()
            CodexTab.QUEST -> searchedQuests.isNotEmpty()
            CodexTab.LOCATION -> searchedLocations.isNotEmpty()
        }

    private fun CodexSearchResults.hasAnyEntry(): Boolean =
        searchedParties.isNotEmpty() ||
            searchedNpcs.isNotEmpty() ||
            searchedQuests.isNotEmpty() ||
            searchedLocations.isNotEmpty()

    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun onEvent(
        event: CodexEvent,
        fields: CodexFields,
        formController: CodexEntryFormController,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
    ) {
        when (event) {
            is CodexEvent.TabSelected -> fields.activeTab.value = event.tab
            is CodexEvent.SearchQueryChanged -> fields.searchQuery.value = event.query
            is CodexEvent.GroupBySelected -> fields.grouping.value = fields.grouping.value.select(event.choice)
            is CodexEvent.EntityClicked -> fields.entitySheet.onEntityClicked(event.ref)
            is CodexEvent.NpcStatusSelected -> fields.entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is CodexEvent.NpcLifeSelected -> fields.entitySheet.onNpcLifeSelected(scope, event.npcId, event.lifeState)
            is CodexEvent.PartyPresenceSelected ->
                fields.entitySheet.onPartyPresenceSelected(scope, event.partyId, event.presence)
            is CodexEvent.QuestStatusSelected ->
                fields.entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is CodexEvent.RelatedNoteClicked -> fields.entitySheet.onRelatedNoteClicked(event.sessionNoteId)
            CodexEvent.SheetDismissed -> fields.entitySheet.onDismissed()
            CodexEvent.AddEntryClicked -> formController.onAddEntryClicked(fields.activeTab.value.toEntryType())
            is CodexEvent.EmptyActionClicked -> formController.onAddEntryClicked(event.entryType, event.name)
            is CodexEvent.EditEntryRequested -> formController.onEditEntryRequested(event.ref, scope)
            is CodexEvent.EntryTypeChanged -> formController.onTypeChanged(event.type)
            is CodexEvent.EntryFieldChanged -> formController.onFieldChanged(event.field, event.text)
            is CodexEvent.EntryNumberChanged -> formController.onNumberChanged(event.field, event.value)
            is CodexEvent.EntryNpcStatusChanged -> formController.onNpcStatusChanged(event.status)
            is CodexEvent.EntryNpcLifeChanged -> formController.onNpcLifeChanged(event.lifeState)
            is CodexEvent.EntryPartyOwnerChanged -> formController.onPartyOwnerChanged(event.isPlayerCharacter)
            is CodexEvent.EntryPartyPresenceChanged -> formController.onPartyPresenceChanged(event.presence)
            is CodexEvent.EntryQuestStatusChanged -> formController.onQuestStatusChanged(event.status)
            is CodexEvent.EntryChipToggled -> formController.onChipToggled(event.field, event.id)
            CodexEvent.EntryFormSaveClicked -> formController.onSaveClicked(scope)
            CodexEvent.EntryFormClosed -> formController.onClosed()
            is CodexEvent.EntityDeleteRequested ->
                onEntityDeleteRequested(event.ref, records, labels, scope)
        }
    }

    // deletes immediately and offers undo - restoring also re-links whatever else referenced the deleted entity
    private fun onEntityDeleteRequested(
        ref: EntityRef,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
    ) {
        val plan = deletionPlanner.planFor(ref, records, labels.entityType) ?: return
        scope.launch {
            plan.delete()
            undoController.show(labels.undoDeletedTitle, plan.subtitle, plan.restore)
        }
    }
}

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
