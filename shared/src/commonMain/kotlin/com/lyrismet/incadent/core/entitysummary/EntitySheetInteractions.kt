package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
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

    fun onQuestStatusSelected(
        scope: CoroutineScope,
        questId: Long,
        status: QuestStatus,
    ) {
        scope.launch { questRepository.updateStatus(questId, status) }
    }

    fun onRelatedNoteClicked(sessionNoteId: Long) {
        selectedRef.value = null
        navigator.goTo(SessionDetailScreen(sessionNoteId))
    }
}
