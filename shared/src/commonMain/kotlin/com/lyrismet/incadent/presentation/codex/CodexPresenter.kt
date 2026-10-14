package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.codexgroup.CodexGroup
import com.lyrismet.incadent.core.codexgroup.CodexGroupLabels
import com.lyrismet.incadent.core.codexgroup.CodexGroupingSelection
import com.lyrismet.incadent.core.codexgroup.CodexRegions
import com.lyrismet.incadent.core.codexgroup.LocationGroupBy
import com.lyrismet.incadent.core.codexgroup.NpcGroupBy
import com.lyrismet.incadent.core.codexgroup.QuestGroupBy
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.entitysummary.buildEntitySummary
import com.lyrismet.incadent.core.entitysummary.entityLookupOf
import com.lyrismet.incadent.core.entitysummary.npcLifeLabels
import com.lyrismet.incadent.core.entitysummary.npcStatusLabels
import com.lyrismet.incadent.core.entitysummary.partyPresenceLabels
import com.lyrismet.incadent.core.entitysummary.questStatusLabels
import com.lyrismet.incadent.core.entitysummary.rememberFullPhoto
import com.lyrismet.incadent.core.entitysummary.withFullPhoto
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.mention.MentionIndex
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.quickedit.InlineEdit
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditInteractions
import com.lyrismet.incadent.core.quickedit.QuickEditKind
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.core.quickedit.QuickPhotoCrop
import com.lyrismet.incadent.core.quickedit.rememberQuickEditFieldTitles
import com.lyrismet.incadent.core.swipehint.SwipeHintTarget
import com.lyrismet.incadent.core.swipehint.codexSwipeHintDirection
import com.lyrismet.incadent.core.swipehint.rememberSwipeHintTarget
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.CampaignStatus
import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.HoldHintState
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.model.SwipeHintState
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_toast_format
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
import dndplayerscodex.shared.generated.resources.codex_group_none
import dndplayerscodex.shared.generated.resources.codex_group_region
import dndplayerscodex.shared.generated.resources.codex_group_relation
import dndplayerscodex.shared.generated.resources.codex_group_status
import dndplayerscodex.shared.generated.resources.codex_group_top_level
import dndplayerscodex.shared.generated.resources.codex_group_tree
import dndplayerscodex.shared.generated.resources.codex_party_card_hp
import dndplayerscodex.shared.generated.resources.codex_party_card_player_prefix
import dndplayerscodex.shared.generated.resources.codex_party_card_you
import dndplayerscodex.shared.generated.resources.codex_undo_deleted_title
import dndplayerscodex.shared.generated.resources.quick_edit_changed_suffix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
private class CodexFields(
    val activeTab: MutableState<CodexTab>,
    val searchQuery: MutableState<String>,
    val grouping: MutableState<CodexGroupingSelection>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
    val inlineEdit: MutableState<InlineEdit?>,
    // the photo waiting in the crop window of the open card, quick mode only
    val photoCrop: MutableState<QuickPhotoCrop?>,
    val quickEdit: QuickEditInteractions,
    val toast: MutableState<CodexToast?>,
    // the places picked or created last, newest first - the select sheet lists them above the rest
    val recentPlaceIds: MutableState<List<Long>>,
    // the places whose tree section is folded on the places tab
    val collapsedPlaceIds: MutableState<Set<Long>>,
    // the record a delete is waiting on - set while the "notes still mention it" warning is up
    val pendingDelete: MutableState<EntityRef?>,
)

/** result of applying the text search query to every list */
internal data class CodexSearchResults(
    val npcsById: Map<Long, Npc>,
    val regions: CodexRegions,
    val searchedParties: List<PartyMember>,
    val searchedNpcs: List<Npc>,
    val searchedQuests: List<Quest>,
    val searchedLocations: List<Location>,
)

/** the lists the active tab renders and what its empty state says */
private class CodexListing(
    val grouped: CodexGroupedLists,
    val emptyKind: CodexEmptyKind,
)

/** the labels the codex screen resolves from resources, collected once per composition */
private class CodexLabels(
    val partyCard: PartyCardLabels,
    val entityType: Map<CodexEntryType, String>,
    val undoDeletedTitle: String,
    val grouped: CodexGroupedLabels,
    val fieldTitles: Map<QuickEditField, String>,
    val changedSuffix: String,
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
    private val imageCompressor: ImageCompressor,
    private val campaignRepository: CampaignRepository,
) : Presenter<CodexState> {
    private val deletionPlanner =
        CodexDeletionPlanner(
            npcRepository,
            partyRepository,
            questRepository,
            locationRepository,
        )

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
        val editMode by appPreferencesRepository.observeEntityEditMode().collectAsState(initial = EntityEditMode.QUICK)
        val holdHint by appPreferencesRepository.observeHoldHintState().collectAsState(initial = HoldHintState.SEEN)
        val undoAction by undoController.current.collectAsState()
        val scope = rememberCoroutineScope()
        val labels = codexLabels()

        val fields = rememberFields(labels)
        val formController = rememberFormController(fields, parties)

        val candidates =
            remember(npcs, locations, quests, parties) {
                MentionIndex(mentionEntitiesFrom(npcs, locations, quests, parties))
            }
        val records = CodexRecords(parties, npcs, quests, locations, sessionEntries, candidates)
        val search = buildSearchResults(records, fields)
        val party = search.searchedParties.toPartyCodexItems(labels.partyCard)
        val listing = codexListing(search, records, party, fields, labels)
        val grouped = listing.grouped
        val emptyKind = listing.emptyKind
        val formState = formController.buildState(records.npcs, records.locations)

        val lookup =
            entityLookupOf(npcs, locations, quests, sessionNotes, sessionEntries, candidates, numbering, parties)
        // keyed on the lookup data, so typing in the open editor doesn't re-parse every session entry
        val selectedRef = fields.selectedEntityRef.value
        val selectedSummary = remember(selectedRef, lookup) { selectedRef?.let { buildEntitySummary(it, lookup) } }
        val fullPhoto = rememberFullPhoto(selectedSummary, npcRepository, partyRepository, locationRepository)
        val selectedEntity = selectedSummary?.withFullPhoto(fullPhoto)

        val isTabSelected = LocalTabSelected.current
        val swipeHintTarget =
            rememberCodexSwipeHintTarget(
                activeTab = fields.activeTab.value,
                party = party,
                grouped = grouped,
                // a hidden tab stays composed in the background, so it must not burn its one-time hint unseen
                isBlocked = formState != null || selectedEntity != null || !isTabSelected,
            )

        return buildCodexState(
            fields,
            formController,
            formState,
            party,
            records,
            grouped,
            emptyKind,
            selectedEntity,
            labels,
            scope,
            CodexEditContext(editMode, holdHint, undoAction, swipeHintTarget),
        )
    }

    @Composable
    private fun rememberFormController(
        fields: CodexFields,
        parties: List<PartyMember>,
    ): CodexEntryFormController =
        CodexEntryFormController.rememberController(
            npcRepository,
            partyRepository,
            questRepository,
            locationRepository,
            fields.recentPlaceIds,
            parties.firstOrNull { it.isPlayerCharacter }?.level,
        )

    // what the active tab lists and its empty state - a tab with no hits lists the other tabs' matches instead
    private fun codexListing(
        search: CodexSearchResults,
        records: CodexRecords,
        party: List<PartyCodexItem>,
        fields: CodexFields,
        labels: CodexLabels,
    ): CodexListing {
        val tab = fields.activeTab.value
        val own =
            codexGroupedLists(
                search,
                records,
                tab,
                fields.grouping.value,
                labels.grouped,
                fields.collapsedPlaceIds.value,
            )
        val emptyKind =
            codexEmptyKind(
                tab,
                fields.searchQuery.value,
                own.hasEntriesFor(tab, party),
                otherSectionMatches(
                    tab,
                    search.searchedParties.size,
                    search.searchedNpcs.size,
                    search.searchedQuests.size,
                    search.searchedLocations.size,
                ),
            )
        val showsOtherSections = emptyKind is CodexEmptyKind.NoMatch && emptyKind.otherMatches > 0
        // ungrouped like the "Все" tab, a grouped tab would repeat an npc met in several places
        val grouped =
            if (showsOtherSections) {
                codexGroupedLists(search, records, CodexTab.ALL, fields.grouping.value, labels.grouped)
            } else {
                own
            }
        return CodexListing(grouped, emptyKind)
    }

    // bundles the pending-state read and the shared trigger so present() doesn't inline both
    @Composable
    private fun rememberCodexSwipeHintTarget(
        activeTab: CodexTab,
        party: List<PartyCodexItem>,
        grouped: CodexGroupedLists,
        isBlocked: Boolean,
    ): SwipeHintTarget<EntityRef>? {
        val swipeHintState by appPreferencesRepository
            .observeCodexSwipeHintState()
            .collectAsState(initial = SwipeHintState.PENDING)
        return rememberSwipeHintTarget(
            hintState = swipeHintState,
            markSeen = { appPreferencesRepository.setCodexSwipeHintState(SwipeHintState.SEEN) },
            undoController = undoController,
            firstItemId = firstCodexEntityRef(activeTab, party, grouped.npcs, grouped.quests, grouped.locations),
            isBlocked = isBlocked,
        )
    }

    // the sheet and the in-place editor share one selection - every committed change is offered to the undo toast
    @Composable
    private fun rememberFields(labels: CodexLabels): CodexFields {
        // rememberRetained not remember - state must survive push/pop navigation, not just recomposition
        val activeTab = rememberRetained { mutableStateOf(CodexTab.ALL) }
        val searchQuery = rememberRetained { mutableStateOf("") }
        val grouping = rememberRetained { mutableStateOf(CodexGroupingSelection()) }
        val selectedEntityRef = rememberRetained { mutableStateOf<EntityRef?>(null) }
        val entityHistory = rememberRetained { mutableStateOf(emptyList<EntityRef>()) }
        val inlineEdit = rememberRetained { mutableStateOf<InlineEdit?>(null) }
        val photoCrop = rememberRetained { mutableStateOf<QuickPhotoCrop?>(null) }
        val entitySheet =
            EntitySheetInteractions(
                selectedEntityRef,
                MentionRepositories(npcRepository, locationRepository, questRepository),
                partyRepository,
                navigator,
                entityHistory,
                onChanged = { change ->
                    val title = labels.fieldTitles.getValue(change.field) + labels.changedSuffix
                    undoController.show(title, change.entityName, change.restore)
                },
            )
        val quickEdit =
            QuickEditInteractions(inlineEdit, photoCrop, selectedEntityRef, entitySheet, imageCompressor) {
                appPreferencesRepository.setHoldHintState(HoldHintState.SEEN)
            }
        val toast = rememberRetained { mutableStateOf<CodexToast?>(null) }
        val recentPlaceIds = rememberRetained { mutableStateOf(emptyList<Long>()) }
        val collapsedPlaceIds = rememberRetained { mutableStateOf(emptySet<Long>()) }
        val pendingDelete = rememberRetained { mutableStateOf<EntityRef?>(null) }
        toast.autoDismiss()
        return CodexFields(
            activeTab,
            searchQuery,
            grouping,
            selectedEntityRef,
            entitySheet,
            inlineEdit,
            photoCrop,
            quickEdit,
            toast,
            recentPlaceIds,
            collapsedPlaceIds,
            pendingDelete,
        )
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
            fieldTitles = rememberQuickEditFieldTitles(),
            changedSuffix = stringResource(Res.string.quick_edit_changed_suffix),
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
                    topLevel = stringResource(Res.string.codex_group_top_level),
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
                            LocationGroupBy.REGION to stringResource(Res.string.codex_group_tree),
                            LocationGroupBy.NONE to stringResource(Res.string.codex_group_none),
                        ),
                ),
            npcLife = npcLifeLabels(),
        )

    // assembles the full screen state from already-computed pieces - a flat data merge, not real complexity
    @Suppress("LongParameterList")
    @Composable
    private fun buildCodexState(
        fields: CodexFields,
        formController: CodexEntryFormController,
        formState: CodexEntryFormState?,
        party: List<PartyCodexItem>,
        records: CodexRecords,
        grouped: CodexGroupedLists,
        emptyKind: CodexEmptyKind,
        selectedEntity: EntitySummaryItem?,
        labels: CodexLabels,
        scope: CoroutineScope,
        context: CodexEditContext,
    ): CodexState {
        val inlineEdit = fields.inlineEdit.value
        val activeTab = fields.activeTab.value
        val campaign by campaignRepository.observeCurrent().collectAsState(initial = null)
        val readOnly = campaign?.status == CampaignStatus.ARCHIVED
        return CodexState(
            readOnly = readOnly,
            activeTab = activeTab,
            searchQuery = fields.searchQuery.value,
            tabCounts =
                CodexTabCounts(
                    party = records.parties.size,
                    npc = records.npcs.size,
                    quest = records.quests.size,
                    location = records.locations.size,
                ),
            party = party,
            npcs = grouped.npcs,
            quests = grouped.quests,
            locations = grouped.locations,
            groupBy = grouped.groupBy,
            groupOptions = grouped.groupOptions,
            emptyState = codexEmptyState(emptyKind, activeTab),
            activeSheet = codexActiveSheet(formState, selectedEntity),
            editMode = context.editMode,
            inlineEdit = inlineEdit,
            photoCrop = fields.photoCrop.value?.draft,
            quickPlaceSelect = quickPlaceSelectState(inlineEdit, fields, records),
            holdTipVisible =
                context.editMode == EntityEditMode.QUICK &&
                    context.holdHint == HoldHintState.PENDING &&
                    inlineEdit == null,
            undoAction = context.undoAction,
            swipeHintTarget = context.swipeHintTarget,
            swipeHintDirection = codexSwipeHintDirection(context.editMode),
            toast = fields.toast.value,
            deleteWarning = deleteWarning(fields.pendingDelete.value, records, labels),
        ) { event ->
            if (!(readOnly && event.writesCampaignData())) {
                onEvent(
                    event,
                    fields,
                    formController,
                    records,
                    labels,
                    scope,
                )
            }
        }
    }

    // the warning of a delete that notes still mention - null when nothing waits or the record is already gone
    private fun deleteWarning(
        ref: EntityRef?,
        records: CodexRecords,
        labels: CodexLabels,
    ): CodexDeleteWarning? {
        val plan = ref?.let { deletionPlanner.planFor(it, records, labels.entityType) } ?: return null
        return CodexDeleteWarning(ref, plan.name, plan.mentionHits.notes, plan.mentionHits.sessions)
    }

    // the select sheet of a place field held on the card - null when no place field is being edited
    @Composable
    private fun quickPlaceSelectState(
        inlineEdit: InlineEdit?,
        fields: CodexFields,
        records: CodexRecords,
    ): PlaceSelectState? {
        val isPlaceEdit = inlineEdit != null && inlineEdit.field.kind is QuickEditKind.Places
        val ref = fields.selectedEntityRef.value
        val target = if (isPlaceEdit) quickPlaceTarget(ref, records) else null
        return target?.let { (field, selectedIds) ->
            placeSelectState(
                field = field,
                title = placeFieldTitle(field),
                locations = records.locations,
                selectedIds = selectedIds,
                editingLocationId = (ref as? EntityRef.Location)?.id,
                recentIds = fields.recentPlaceIds.value,
            )
        }
    }

    private fun buildSearchResults(
        records: CodexRecords,
        fields: CodexFields,
    ): CodexSearchResults {
        val query = fields.searchQuery.value.trim()
        val npcsById = records.npcs.associateBy { it.id }
        val regions = CodexRegions(records.npcs, records.locations)
        return CodexSearchResults(
            npcsById = npcsById,
            regions = regions,
            searchedParties = records.parties.filter { it.matchesQuery(query) },
            searchedNpcs = records.npcs.filter { it.matchesQuery(query, regions) },
            searchedQuests = records.quests.filter { it.matchesQuery(query, regions) },
            searchedLocations = records.locations.filter { it.matchesQuery(query, regions) },
        )
    }

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
            is CodexEvent.EntityClicked -> {
                fields.quickEdit.onSheetClosed()
                fields.entitySheet.onEntityClicked(event.ref)
            }
            is CodexEvent.NpcStatusSelected -> fields.entitySheet.onNpcStatusSelected(scope, event.npcId, event.status)
            is CodexEvent.NpcLifeSelected -> fields.entitySheet.onNpcLifeSelected(scope, event.npcId, event.lifeState)
            is CodexEvent.PartyPresenceSelected ->
                fields.entitySheet.onPartyPresenceSelected(scope, event.partyId, event.presence)
            is CodexEvent.QuestStatusSelected ->
                fields.entitySheet.onQuestStatusSelected(scope, event.questId, event.status)
            is CodexEvent.RelatedNoteClicked ->
                fields.entitySheet.onRelatedNoteClicked(event.sessionNoteId, event.entryId)
            CodexEvent.SheetDismissed -> {
                fields.entitySheet.onDismissed()
                fields.quickEdit.onSheetClosed()
            }
            is CodexEvent.QuickEdit -> {
                val answer = event.event
                if (answer is QuickEditUiEvent.PlacesChosen) fields.rememberRecentPlaces(answer.ids)
                fields.quickEdit.onEvent(scope, answer)
            }
            is CodexEvent.QuickPlaceCreated -> onQuickPlaceCreated(event, fields, scope)
            CodexEvent.UndoClicked -> undoController.undo()
            CodexEvent.AddEntryClicked -> formController.onAddEntryClicked(fields.activeTab.value.toEntryType())
            is CodexEvent.EmptyActionClicked -> formController.onAddEntryClicked(event.entryType, event.name)
            is CodexEvent.EditEntryRequested -> formController.onEditEntryRequested(event.ref, scope)
            is CodexEvent.PlaceSectionToggled -> {
                val folded = fields.collapsedPlaceIds.value
                fields.collapsedPlaceIds.value =
                    if (event.placeId in folded) folded - event.placeId else folded + event.placeId
            }
            is CodexEntryFormEvent -> {
                val closed =
                    formController.onEvent(event, scope, imageCompressor) { name ->
                        fields.toast.value = CodexToast(getString(Res.string.codex_entry_quick_add_toast_format, name))
                    }
                if (closed) fields.toast.value = null
            }
            is CodexEvent.EntityDeleteRequested -> onEntityDeleteRequested(event.ref, fields, records, labels, scope)
            CodexEvent.EntityDeleteConfirmed ->
                fields.pendingDelete.value?.let { ref ->
                    fields.pendingDelete.value = null
                    deleteNow(ref, records, labels, scope)
                }
            CodexEvent.EntityDeleteCancelled -> fields.pendingDelete.value = null
        }
    }

    // the new place joins what was ticked in the card's select sheet, a single field takes just the new place
    private fun onQuickPlaceCreated(
        event: CodexEvent.QuickPlaceCreated,
        fields: CodexFields,
        scope: CoroutineScope,
    ) {
        val name = event.name.trim().capitalizeFirst()
        if (name.isBlank()) return
        val isMulti = fields.selectedEntityRef.value is EntityRef.Npc
        scope.launch {
            val id = locationRepository.upsert(Location(id = 0, name = name, description = ""))
            val ids = if (isMulti) event.checked + id else listOf(id)
            fields.rememberRecentPlaces(listOf(id))
            fields.quickEdit.onEvent(scope, QuickEditUiEvent.PlacesChosen(ids))
        }
    }

    // a record notes still mention asks first, anything else goes straight away
    private fun onEntityDeleteRequested(
        ref: EntityRef,
        fields: CodexFields,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
    ) {
        val plan = deletionPlanner.planFor(ref, records, labels.entityType) ?: return
        if (plan.mentionHits.notes > 0) fields.pendingDelete.value = ref else deleteNow(ref, records, labels, scope)
    }

    // deletes immediately and offers undo - restoring also re-links whatever else referenced the deleted entity
    private fun deleteNow(
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

private const val MAX_RECENT_PLACES = 12

private fun CodexFields.rememberRecentPlaces(ids: List<Long>) {
    recentPlaceIds.value = (ids + recentPlaceIds.value).distinct().take(MAX_RECENT_PLACES)
}

/** the edit preferences and the undo toast the screen state is assembled from */
private data class CodexEditContext(
    val editMode: EntityEditMode,
    val holdHint: HoldHintState,
    val undoAction: UndoAction?,
    val swipeHintTarget: SwipeHintTarget<EntityRef>?,
)

// priority mirrors the mockup's firstC - and CodexAllList's own party/npc/quest/location render order
private fun firstCodexEntityRef(
    activeTab: CodexTab,
    party: List<PartyCodexItem>,
    npcs: List<CodexGroup<NpcCodexItem>>,
    quests: List<CodexGroup<QuestCodexItem>>,
    locations: List<CodexGroup<LocationCodexItem>>,
): EntityRef? {
    val firstParty = party.firstOrNull()?.let { EntityRef.Party(it.id) }
    val firstNpc =
        npcs
            .firstOrNull()
            ?.items
            ?.firstOrNull()
            ?.let { EntityRef.Npc(it.id) }
    val firstQuest =
        quests
            .firstOrNull()
            ?.items
            ?.firstOrNull()
            ?.let { EntityRef.Quest(it.id) }
    val firstLocation =
        locations
            .firstOrNull()
            ?.items
            ?.firstOrNull()
            ?.let { EntityRef.Location(it.id) }
    return when (activeTab) {
        CodexTab.ALL -> firstParty ?: firstNpc ?: firstQuest ?: firstLocation
        CodexTab.PARTY -> firstParty
        CodexTab.NPC -> firstNpc
        CodexTab.QUEST -> firstQuest
        CodexTab.LOCATION -> firstLocation
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
