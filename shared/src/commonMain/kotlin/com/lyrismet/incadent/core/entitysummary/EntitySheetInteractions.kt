package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.updateLifeState
import com.lyrismet.incadent.domain.repository.updatePresence
import com.lyrismet.incadent.domain.repository.updateStatus
import com.lyrismet.incadent.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** the tap-a-chip/dismiss/change-status/follow-a-related-note flow every screen hosting the entity sheet reuses */
class EntitySheetInteractions(
    private val selectedRef: MutableState<EntityRef?>,
    private val npcRepository: NpcRepository,
    private val questRepository: QuestRepository,
    private val partyRepository: PartyRepository,
    private val navigator: Navigator,
) {
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
        scope.launch { npcRepository.updateStatus(npcId, status) }
    }

    fun onNpcLifeSelected(
        scope: CoroutineScope,
        npcId: Long,
        lifeState: NpcLifeState,
    ) {
        scope.launch { npcRepository.updateLifeState(npcId, lifeState) }
    }

    fun onQuestStatusSelected(
        scope: CoroutineScope,
        questId: Long,
        status: QuestStatus,
    ) {
        scope.launch { questRepository.updateStatus(questId, status) }
    }

    fun onPartyPresenceSelected(
        scope: CoroutineScope,
        partyId: Long,
        presence: PartyPresence,
    ) {
        scope.launch { partyRepository.updatePresence(partyId, presence) }
    }

    fun onRelatedNoteClicked(sessionNoteId: Long) {
        selectedRef.value = null
        navigator.goTo(SessionDetailScreen(sessionNoteId))
    }
}
