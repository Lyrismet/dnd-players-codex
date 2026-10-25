package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.debounce.rememberDebounced
import com.lyrismet.incadent.core.errors.ReportFailures
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.EntityEditMode
import com.lyrismet.incadent.core.preferences.HoldHintState
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.preferences.SwipeHintState
import com.lyrismet.incadent.core.swipehint.SwipeHintTarget
import com.lyrismet.incadent.core.swipehint.codexSwipeHintDirection
import com.lyrismet.incadent.core.swipehint.rememberSwipeHintTarget
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroup
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroupingSelection
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexRegions
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.LocationRepository
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.codex.domain.repository.QuestRepository
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormState
import com.lyrismet.incadent.feature.codex.presentation.form.rememberPlaceFailureText
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EntityChange
import com.lyrismet.incadent.feature.codex.presentation.quickedit.InlineEdit
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditInteractions
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickPhotoCrop
import com.lyrismet.incadent.feature.codex.presentation.quickedit.rememberEntityChangeReporter
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityLookup
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.codex.presentation.summary.creatureTypeLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.entityLookupOf
import com.lyrismet.incadent.feature.codex.presentation.summary.rememberEntitySummary
import com.lyrismet.incadent.feature.codex.presentation.summary.rememberFullPhoto
import com.lyrismet.incadent.feature.codex.presentation.summary.withFullPhoto
import com.lyrismet.incadent.feature.combat.domain.repository.CombatFactsSource
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.mention.domain.MentionBody
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.mentionSessionCounts
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import com.lyrismet.incadent.feature.session.presentation.route.sessionEntryOpener
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_party_fell_session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private fun mentionIndexOf(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
    species: List<Species>,
) = MentionIndex(mentionEntitiesFrom(npcs, locations, quests, parties, species))

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

@Suppress("LongParameterList")
class CodexPresenter(
    private val navigator: Navigator,
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
    private val speciesRepository: SpeciesRepository,
    private val combatFactsSource: CombatFactsSource,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val imageCompressor: ImageCompressor,
    private val campaignRepository: CampaignRepository,
) : Presenter<CodexState> {
    private val codexRepositories =
        CodexRepositories(partyRepository, npcRepository, questRepository, locationRepository, speciesRepository)
    private val deletionPlanner =
        CodexDeletionPlanner(
            npcRepository,
            partyRepository,
            questRepository,
            locationRepository,
            speciesRepository,
        )

    private val events = CodexEventHandler(imageCompressor, undoController, deletionPlanner)

    // one collection per source the screen reads - a flat merge, not real complexity
    @Suppress("LongMethod")
    @Composable
    override fun present(): CodexState {
        val loadedParties by partyRepository.observeAll().collectAsState(initial = null)
        val loadedNpcs by npcRepository.observeAll().collectAsState(initial = null)
        val parties = loadedParties.orEmpty()
        val npcs = loadedNpcs.orEmpty()
        val species by speciesRepository.observeAll().collectAsState(initial = emptyList())
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
        val failure = remember { mutableStateOf<Throwable?>(null) }
        val scope = rememberGuardedScope { failure.value = it }
        val labels = codexLabels()

        val fields = rememberFields(rememberEntityChangeReporter(undoController))
        ReportFailures(failure, rememberPlaceFailureText()) { fields.toast.value = ToastMessage(it) }
        val formController = rememberFormController(fields, parties)

        val records = rememberRecords(parties, npcs, quests, locations, sessionEntries, species)
        // the field shows every keystroke at once, the lists below follow once typing pauses
        val query = rememberDebounced(fields.searchQuery.value.trim()) { it.isEmpty() }
        val search = remember(records, query) { buildSearchResults(records, query) }
        val fell = fellIn(records, sessionNotes, numbering)
        val party =
            remember(search, labels, fell) { search.searchedParties.toPartyCodexItems(labels.partyCard, fell) }
        // the lists are rebuilt when what they are made of changes, not on every keystroke of a form or an editor
        val tab = fields.activeTab.value
        val grouping = fields.grouping.value
        val collapsed = fields.collapsedPlaceIds.value
        val npcMode = fields.npcMode.value
        val listing =
            remember(search, records, party, labels, query, tab, grouping, collapsed, npcMode) {
                codexListing(search, records, party, fields, labels, query)
            }
        val formState = formController.buildState(records.npcs, records.locations, species)

        val lookup = lookupOf(records, sessionNotes, numbering)
        val selectedEntity = selectedEntity(fields.selectedEntityRef.value, lookup)

        val swipeHintTarget =
            rememberCodexSwipeHintTarget(
                activeTab = fields.activeTab.value,
                party = party,
                grouped = listing.grouped,
                // a hidden tab stays composed in the background, so it must not burn its one-time hint unseen
                isBlocked =
                    formState != null || selectedEntity != null || !LocalTabSelected.current || fields.showsSpecies,
            )

        return buildCodexState(
            fields,
            formController,
            formState,
            party,
            records,
            listing.grouped,
            listing.emptyKind,
            selectedEntity,
            labels,
            scope,
            query,
            loadedParties == null || loadedNpcs == null,
            CodexEditContext(editMode, holdHint, undoAction, swipeHintTarget),
        )
    }

    // the session each fallen character fell in, by their id
    @Composable
    private fun fellIn(
        records: CodexRecords,
        sessionNotes: List<SessionNote>,
        numbering: SessionNumbering,
    ): Map<Long, String> {
        val numbers = sessionNotes.sessionNumberLabels(numbering)
        return records.parties
            .filter { it.fallen }
            .mapNotNull { member ->
                val session = member.fellCombatId?.let { records.facts.falls[it] }?.sessionNoteId
                numbers[session]?.let { member.id to stringResource(Res.string.codex_party_fell_session, it) }
            }.toMap()
    }

    @Composable
    private fun lookupOf(
        records: CodexRecords,
        sessionNotes: List<SessionNote>,
        numbering: SessionNumbering,
    ) = entityLookupOf(
        records.npcs,
        records.locations,
        records.quests,
        sessionNotes,
        records.entries,
        records.mentionIndex,
        numbering,
        records.parties,
        records.species,
        records.facts,
    )

    // the mention candidates are rebuilt only when a record list changes
    @Composable
    private fun rememberRecords(
        parties: List<PartyMember>,
        npcs: List<Npc>,
        quests: List<Quest>,
        locations: List<Location>,
        entries: List<SessionEntry>,
        species: List<Species>,
    ): CodexRecords {
        val facts by combatFactsSource.observe().collectAsState(initial = CombatFacts.EMPTY)
        val index =
            remember(
                npcs,
                locations,
                quests,
                parties,
                species,
            ) { mentionIndexOf(npcs, locations, quests, parties, species) }
        return remember(parties, npcs, quests, locations, entries, index, species, facts) {
            CodexRecords(parties, npcs, quests, locations, entries, index, species, facts)
        }
    }

    // keyed on the lookup data, so typing in the open editor doesn't re-parse every session entry
    @Composable
    private fun selectedEntity(
        ref: EntityRef?,
        lookup: EntityLookup,
    ): EntitySummaryItem? {
        val summary = rememberEntitySummary(ref, lookup)
        return summary?.withFullPhoto(rememberFullPhoto(summary, codexRepositories))
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
            speciesRepository,
            fields.recentPlaceIds,
            parties.firstOrNull { it.isPlayerCharacter }?.level,
            parties.any { it.isPlayerCharacter },
        )

    // what the active tab lists and its empty state - a tab with no hits lists the other tabs' matches instead
    private fun codexListing(
        search: CodexSearchResults,
        records: CodexRecords,
        party: List<PartyCodexItem>,
        fields: CodexFields,
        labels: CodexLabels,
        query: String,
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
        val speciesMatches = records.species.count { it.matchesQuery(query) }
        val emptyKind =
            codexEmptyKind(
                tab,
                query,
                if (fields.showsSpecies) speciesMatches > 0 else own.hasEntriesFor(tab, party, speciesMatches > 0),
                otherSectionMatches(
                    tab,
                    search.searchedParties.size,
                    search.searchedNpcs.size,
                    search.searchedQuests.size,
                    search.searchedLocations.size,
                    speciesMatches,
                    fields.npcMode.value,
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
    private fun rememberFields(reportChange: suspend (EntityChange) -> Unit): CodexFields {
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
                codexRepositories,
                navigator.sessionEntryOpener(),
                entityHistory,
                onChanged = reportChange,
            )
        val recentPlaceIds = rememberRetained { mutableStateOf(emptyList<Long>()) }
        val quickEdit =
            QuickEditInteractions(
                inlineEdit,
                photoCrop,
                selectedEntityRef,
                entitySheet,
                imageCompressor,
                recentPlaceIds,
                codexRepositories,
                undoController,
            ) {
                appPreferencesRepository.setHoldHintState(HoldHintState.SEEN)
            }
        val toast = rememberRetained { mutableStateOf<ToastMessage?>(null) }
        val collapsedPlaceIds = rememberRetained { mutableStateOf(emptySet<Long>()) }
        val pendingDelete = rememberRetained { mutableStateOf<EntityRef?>(null) }
        val npcMode = rememberRetained { mutableStateOf(NpcListMode.NAMED) }
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
            npcMode,
        )
    }

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
        query: String,
        isLoading: Boolean,
        context: CodexEditContext,
    ): CodexState {
        val activeTab = fields.activeTab.value
        val campaign by campaignRepository.observeCurrent().collectAsState(initial = null)
        val readOnly = campaign?.status == CampaignStatus.ARCHIVED
        // one identity for the whole screen, so the rows do not recompose when only the data behind the sink moved
        val latest =
            rememberUpdatedState { event: CodexEvent ->
                if (!(readOnly && event.writesCampaignData())) {
                    events.handle(event, fields, formController, records, labels, scope)
                }
            }
        val eventSink = remember { { event: CodexEvent -> latest.value(event) } }
        return CodexState(
            readOnly = readOnly,
            isLoading = isLoading,
            activeTab = activeTab,
            searchQuery = fields.searchQuery.value,
            tabCounts =
                CodexTabCounts(
                    party = records.parties.size,
                    npc = records.npcs.size,
                    species = records.species.size,
                    quest = records.quests.size,
                    location = records.locations.size,
                ),
            party = party,
            npcs = grouped.npcs,
            npcMode = fields.npcMode.value,
            species =
                speciesItems(
                    records,
                    query,
                    isShown = activeTab == CodexTab.ALL || fields.showsSpecies,
                ),
            quests = grouped.quests,
            locations = grouped.locations,
            groupBy = grouped.groupBy,
            groupOptions = grouped.groupOptions,
            emptyState = codexEmptyState(emptyKind, activeTab),
            activeSheet = codexActiveSheet(formState, selectedEntity, fields.selectedEntityRef.value != null),
            editMode = context.editMode,
            quick =
                fields.quickEdit.state(
                    records,
                    holdTipAllowed =
                        context.editMode == EntityEditMode.QUICK && context.holdHint == HoldHintState.PENDING,
                    undoAction = context.undoAction,
                ),
            swipeHintTarget = context.swipeHintTarget,
            swipeHintDirection = codexSwipeHintDirection(context.editMode),
            toast = fields.toast.value,
            deleteWarning = deleteWarning(fields.pendingDelete.value, records, labels),
            eventSink = eventSink,
        )
    }

    // the species rows of the npc tab, each with the number of sessions that mention it
    @Composable
    private fun speciesItems(
        records: CodexRecords,
        query: String,
        isShown: Boolean,
    ): List<SpeciesCodexItem> {
        val typeLabels = creatureTypeLabels()
        // parsing every note is only worth it while the species rows can be seen, and it runs off the main thread
        val visible = isShown && LocalTabSelected.current
        val seen by produceState(emptyMap<Long, Int>(), records.entries, records.mentionIndex, visible) {
            if (visible) {
                value =
                    withContext(Dispatchers.Default) {
                        val bodies = records.entries.map { MentionBody(it.sessionNoteId, it.body) }
                        mentionSessionCounts(bodies, records.mentionIndex)
                            .filterKeys { it.type == EntityKind.SPECIES }
                            .mapKeys { it.key.id }
                    }
            }
        }
        return remember(
            records.species,
            query,
            typeLabels,
            seen,
            records.facts,
        ) { records.species.toCodexItems(query, typeLabels, seen, records.facts.species) }
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

    private fun buildSearchResults(
        records: CodexRecords,
        query: String,
    ): CodexSearchResults {
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
    entitySelected: Boolean,
): CodexSheet? =
    when {
        formState != null -> CodexSheet.EntryForm(formState)
        selectedEntity != null -> CodexSheet.EntityView(selectedEntity)
        entitySelected -> CodexSheet.EntityLoading
        else -> null
    }
