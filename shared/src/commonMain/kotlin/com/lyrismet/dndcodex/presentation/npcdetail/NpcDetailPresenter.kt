package com.lyrismet.dndcodex.presentation.npcdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter

class NpcDetailPresenter(
    private val screen: NpcDetailScreen,
    private val navigator: Navigator,
    private val npcRepository: NpcRepository,
) : Presenter<NpcDetailState> {
    @Composable
    override fun present(): NpcDetailState {
        val npc by produceState<Npc?>(initialValue = null, key1 = screen.npcId) {
            value = npcRepository.getById(screen.npcId)
        }

        return NpcDetailState(npc = npc, isLoading = npc == null) { event ->
            when (event) {
                NpcDetailEvent.BackClicked -> navigator.pop()
            }
        }
    }
}
