package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.quickedit.EntityChange
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditValue
import com.lyrismet.incadent.core.quickedit.withGiverToggled
import com.lyrismet.incadent.core.quickedit.withLifeState
import com.lyrismet.incadent.core.quickedit.withPlaceToggled
import com.lyrismet.incadent.core.quickedit.withPresence
import com.lyrismet.incadent.core.quickedit.withQuickEdit
import com.lyrismet.incadent.core.quickedit.withStatus
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// process-wide since the sheet is rebuilt on every composition, so an instance lock would serialize nothing
private val entityWriteLock = Mutex()

/** the tap/dismiss/change-value/related-note flow every entity-sheet host reuses - [onChanged] feeds the undo toast */
class EntitySheetInteractions(
    private val selectedRef: MutableState<EntityRef?>,
    repositories: MentionRepositories,
    private val partyRepository: PartyRepository,
    private val navigator: Navigator,
    // the entities left open underneath, newest last - the host keeps it alive across recompositions
    private val history: MutableState<List<EntityRef>> = mutableStateOf(emptyList()),
    private val onChanged: (EntityChange) -> Unit = {},
) {
    private val npcRepository = repositories.npcRepository
    private val questRepository = repositories.questRepository
    private val locationRepository = repositories.locationRepository

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
                    commitChange(
                        field,
                        { locationRepository.getById(ref.id) },
                        Location::name,
                        { it.withQuickEdit(field, value) },
                    ) { locationRepository.upsert(it) }
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
        navigator.goTo(SessionDetailScreen(sessionNoteId, focusEntryId = entryId))
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
