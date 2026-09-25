package com.lyrismet.dndcodex.presentation.npclist

import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addNpcListUi(npcRepository: NpcRepository): Circuit.Builder =
    addPresenter<NpcListScreen, NpcListState> { _, navigator, _ ->
        NpcListPresenter(navigator, npcRepository)
    }.addUi<NpcListScreen, NpcListState> { state, modifier ->
        NpcListUi(state, modifier)
    }

val npcListScreenRegistration =
    CircuitSerializerRegistration { it.subclass(NpcListScreen::class, NpcListScreen.serializer()) }
