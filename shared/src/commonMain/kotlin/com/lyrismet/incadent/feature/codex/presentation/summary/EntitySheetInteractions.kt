package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.feature.codex.domain.grouping.InsideChanges
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.grouping.with
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.quickedit.EntityChange
import com.lyrismet.incadent.feature.codex.presentation.quickedit.MovedPlaces
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditValue
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withGiverToggled
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withLifeState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withPlaceToggled
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withPresence
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withQuickEdit
import com.lyrismet.incadent.feature.codex.presentation.quickedit.withStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// process-wide since the sheet is rebuilt on every composition, so an instance lock would serialize nothing
internal val entityWriteLock = Mutex()

/** the tap/dismiss/change-value/related-note flow every entity-sheet host reuses - [onChanged] feeds the undo toast */
class EntitySheetInteractions(
    private val selectedRef: MutableState<EntityRef?>,
    repositories: CodexRepositories,
    // opens a session at one of its notes - the host decides how, so this stays free of any screen
    private val openSession: (sessionNoteId: Long, entryId: Long) -> Unit,
    // the entities left open underneath, newest last - the host keeps it alive across recompositions
    private val history: MutableState<List<EntityRef>> = mutableStateOf(emptyList()),
    private val onChanged: suspend (EntityChange) -> Unit = {},
    // null on a screen that offers no links from notes
    internal val links: LinkSuggestionInteractions? = null,
) {
    private val partyRepository = repositories.partyRepository
    private val npcRepository = repositories.npcRepository
    private val questRepository = repositories.questRepository
    private val locationRepository = repositories.locationRepository
    private val speciesRepository = repositories.speciesRepository

    // opening an entity from inside another one's sheet keeps that one underneath
    fun onEntityClicked(ref: EntityRef) {
        val current = selectedRef.value
        if (current != null && current != ref) history.value = history.value + current
        selectedRef.value = ref
    }

    // closing goes back to the entity that was open before, the last one closes the sheet
    fun onDismissed() {
        val previous = history.value.lastOrNull()
        history.value = history.value.dropLast(1)
        selectedRef.value = previous
    }

    // a deleted card leaves the stack, and the sheet steps back when it was the open one
    fun onEntityRemoved(ref: EntityRef) {
        history.value = history.value.filter { it != ref }
        if (selectedRef.value == ref) onDismissed()
    }

    fun onNpcStatusSelected(
        scope: CoroutineScope,
        npcId: Long,
        status: NpcStatus,
    ) {
        scope.launch {
            commitChange(
                QuickEditField.RELATION,
                { npcRepository.getById(npcId) },
                Npc::name,
                { it.withStatus(status) },
            ) { npcRepository.upsert(it) }
        }
    }

    fun onNpcLifeSelected(
        scope: CoroutineScope,
        npcId: Long,
        lifeState: NpcLifeState,
    ) {
        scope.launch {
            commitChange(
                QuickEditField.LIFE,
                { npcRepository.getById(npcId) },
                Npc::name,
                { it.withLifeState(lifeState) },
            ) { npcRepository.upsert(it) }
        }
    }

    fun onQuestStatusSelected(
        scope: CoroutineScope,
        questId: Long,
        status: QuestStatus,
    ) {
        scope.launch {
            commitChange(
                QuickEditField.QUEST_STATUS,
                { questRepository.getById(questId) },
                Quest::title,
                { it.withStatus(status) },
            ) { questRepository.upsert(it) }
        }
    }

    fun onSpeciesVulnerabilityChanged(
        scope: CoroutineScope,
        speciesId: Long,
        type: DamageType,
        state: DamageState,
    ) {
        scope.launch {
            entityWriteLock.withLock {
                speciesRepository
                    .getById(
                        speciesId,
                    )?.let { speciesRepository.upsert(it.copy(vulnerabilities = it.vulnerabilities.with(type, state))) }
            }
        }
    }

    fun onNpcVulnerabilityChanged(
        scope: CoroutineScope,
        npcId: Long,
        type: DamageType,
        state: DamageState,
    ) {
        scope.launch {
            entityWriteLock.withLock {
                npcRepository
                    .getById(npcId)
                    ?.let { npcRepository.upsert(it.copy(vulnerabilities = it.vulnerabilities.with(type, state))) }
            }
        }
    }

    /** reports the whole editing as one change that puts back what the npc had when it began */
    fun onNpcVulnerabilitiesEdited(
        scope: CoroutineScope,
        npcId: Long,
        before: Map<DamageType, DamageState>,
    ) {
        scope.launch {
            entityWriteLock.withLock {
                val npc = npcRepository.getById(npcId) ?: return@withLock
                if (npc.vulnerabilities == before) return@withLock
                val after = npc.vulnerabilities
                onChanged(
                    EntityChange(QuickEditField.VULNERABILITIES, npc.name) {
                        entityWriteLock.withLock {
                            npcRepository.getById(npcId)?.takeIf { it.vulnerabilities == after }?.let {
                                npcRepository.upsert(it.copy(vulnerabilities = before))
                            }
                        }
                    },
                )
            }
        }
    }

    fun onPartyPresenceSelected(
        scope: CoroutineScope,
        partyId: Long,
        presence: PartyPresence,
    ) {
        scope.launch {
            commitChange(
                QuickEditField.PRESENCE,
                { partyRepository.getById(partyId) },
                PartyMember::name,
                { it.withPresence(presence) },
            ) { partyRepository.upsert(it) }
        }
    }

    // a fallen character is back in the party, the sheet shows them alive when the record changes
    fun onPartyRevived(
        scope: CoroutineScope,
        partyId: Long,
    ) {
        scope.launch { entityWriteLock.withLock { partyRepository.setFallen(partyId, false) } }
    }

    /** writes one typed or tapped field - an unknown field for the entity type is silently ignored */
    fun onQuickEdit(
        scope: CoroutineScope,
        ref: EntityRef,
        field: QuickEditField,
        value: QuickEditValue,
    ) {
        scope.launch {
            when (ref) {
                is EntityRef.Npc ->
                    commitChange(
                        field,
                        { npcRepository.getById(ref.id) },
                        Npc::name,
                        { it.withQuickEdit(field, value) },
                    ) { npcRepository.upsert(it) }
                is EntityRef.Quest ->
                    commitChange(
                        field,
                        { questRepository.getById(ref.id) },
                        Quest::title,
                        { it.withQuickEdit(field, value) },
                    ) { questRepository.upsert(it) }
                is EntityRef.Location ->
                    if (field == QuickEditField.INSIDE) {
                        commitInside(ref.id, value)
                    } else {
                        commitChange(
                            field,
                            { locationRepository.getById(ref.id) },
                            Location::name,
                            { it.withQuickEdit(field, value) },
                        ) { locationRepository.upsert(it) }
                    }
                is EntityRef.Species ->
                    commitChange(
                        field,
                        { speciesRepository.getById(ref.id) },
                        Species::name,
                        { it.withQuickEdit(field, value) },
                    ) { speciesRepository.upsert(it) }
                is EntityRef.Party ->
                    commitChange(
                        field,
                        { partyRepository.getById(ref.id) },
                        PartyMember::name,
                        { it.withQuickEdit(field, value) },
                    ) { partyRepository.upsert(it) }
            }
        }
    }

    /** ticks or unticks one candidate of a relation group - the change lands on the candidate, not on [owner] */
    fun onRelationToggled(
        scope: CoroutineScope,
        owner: EntityRef,
        field: QuickEditField,
        targetId: Long,
    ) {
        scope.launch {
            when {
                field == QuickEditField.GIVEN_QUESTS && owner is EntityRef.Npc ->
                    commitQuestChange(field, targetId) { it.withGiverToggled(owner.id) }
                field == QuickEditField.PLACE_QUESTS && owner is EntityRef.Location ->
                    commitQuestChange(field, targetId) { it.withPlaceToggled(owner.id) }
                field == QuickEditField.RESIDENTS && owner is EntityRef.Location ->
                    commitChange(
                        field,
                        { npcRepository.getById(targetId) },
                        Npc::name,
                        { it.withPlaceToggled(owner.id) },
                    ) { npcRepository.upsert(it) }
            }
        }
    }

    // picking the inside of a place sets the parent of those places - the ones dropped from the pick go to the top
    private suspend fun commitInside(
        locationId: Long,
        value: QuickEditValue,
    ) {
        val picked = (value as? QuickEditValue.Places)?.ids ?: return
        entityWriteLock.withLock {
            val all = locationRepository.observeAll().first()
            val tree = PlaceTree(all)
            val changes = tree.insideChanges(locationId, picked)
            val owner = tree.find(locationId)
            if (owner != null && !changes.isEmpty) {
                val before = (changes.entering + changes.leaving).associateWith { tree.find(it)?.parentId }
                locationRepository.setParents(
                    changes.leaving.associateWith { null } + changes.entering.associateWith { locationId },
                )
                val sources =
                    changes.entering
                        .map {
                            tree
                                .find(it)
                                ?.parentId
                                ?.let(tree::find)
                                ?.name
                        }.distinct()
                val moved = MovedPlaces(changes.entering.size, sources.singleOrNull())
                onChanged(
                    EntityChange(QuickEditField.INSIDE, owner.name, moved) {
                        entityWriteLock.withLock { restoreParents(locationId, changes, before) }
                    },
                )
            }
        }
    }

    // a place moved again since keeps its newer parent, one whose old parent is gone or would now loop stays put
    private suspend fun restoreParents(
        locationId: Long,
        changes: InsideChanges,
        before: Map<Long, Long?>,
    ) {
        val tree = PlaceTree(locationRepository.observeAll().first())
        val restores =
            before.filter { (id, oldParent) ->
                val placedBy = if (id in changes.entering) locationId else null
                tree.find(id)?.parentId == placedBy &&
                    (oldParent == null || tree.find(oldParent) != null) &&
                    tree.canPutInside(id, oldParent)
            }
        locationRepository.setParents(restores)
    }

    private suspend fun commitQuestChange(
        field: QuickEditField,
        questId: Long,
        change: (Quest) -> Quest,
    ) = commitChange(field, { questRepository.getById(questId) }, Quest::title, change) { questRepository.upsert(it) }

    fun onRelatedNoteClicked(
        sessionNoteId: Long,
        entryId: Long,
    ) {
        selectedRef.value = null
        history.value = emptyList()
        openSession(sessionNoteId, entryId)
    }

    // a no-op change (same value, unknown field, missing record) writes nothing and offers no undo
    private suspend fun <T> commitChange(
        field: QuickEditField,
        read: suspend () -> T?,
        nameOf: (T) -> String,
        change: (T) -> T?,
        write: suspend (T) -> Unit,
    ) {
        entityWriteLock.withLock {
            val before = read() ?: return
            val after = change(before)?.takeIf { it != before } ?: return
            write(after)
            onChanged(
                EntityChange(field, nameOf(before)) {
                    // a record changed again since this edit keeps its newer state instead of reverting to the snapshot
                    entityWriteLock.withLock {
                        if (read() == after) write(before)
                    }
                },
            )
        }
    }
}
