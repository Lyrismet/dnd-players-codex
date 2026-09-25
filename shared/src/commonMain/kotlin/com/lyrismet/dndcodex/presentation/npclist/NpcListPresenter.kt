package com.lyrismet.dndcodex.presentation.npclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.lyrismet.dndcodex.presentation.npcdetail.NpcDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

class NpcListPresenter(
    private val navigator: Navigator,
    private val npcRepository: NpcRepository,
) : Presenter<NpcListState> {
    @Composable
    override fun present(): NpcListState {
        val npcs by npcRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        return NpcListState(npcs = npcs) { event ->
            when (event) {
                is NpcListEvent.NpcClicked -> navigator.goTo(NpcDetailScreen(event.npcId))
                NpcListEvent.AddSampleNpcClicked ->
                    scope.launch {
                        npcRepository.upsert(
                            Npc(
                                id = 0,
                                name = "New NPC ${npcs.size + 1}",
                                status = NpcStatus.NEUTRAL,
                                description = "",
                                locationId = null,
                            ),
                        )
                    }
            }
        }
    }
}
