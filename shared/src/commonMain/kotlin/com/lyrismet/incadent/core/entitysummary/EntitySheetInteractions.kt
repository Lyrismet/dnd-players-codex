package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.quickedit.EntityChange
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditValue
import com.lyrismet.incadent.core.quickedit.withLifeState
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
    private val onChanged: (EntityChange) -> Unit = {},
) {
    private val npcRepository = repositories.npcRepository
    private val questRepository = repositories.questRepository
    private val locationRepository = repositories.locationRepository

    fun onEntityClicked(ref: EntityRef) {
        selectedRef.value = ref
    }

    fun onDismissed() {
        selectedRef.value = null
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

    fun onRelatedNoteClicked(sessionNoteId: Long) {
        selectedRef.value = null
        navigator.goTo(SessionDetailScreen(sessionNoteId))
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
