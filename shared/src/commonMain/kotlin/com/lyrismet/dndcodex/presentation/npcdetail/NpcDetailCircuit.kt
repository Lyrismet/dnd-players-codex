package com.lyrismet.dndcodex.presentation.npcdetail

import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addNpcDetailUi(npcRepository: NpcRepository): Circuit.Builder =
    addPresenter<NpcDetailScreen, NpcDetailState> { screen, navigator, _ ->
        NpcDetailPresenter(screen, navigator, npcRepository)
    }.addUi<NpcDetailScreen, NpcDetailState> { state, modifier ->
        NpcDetailUi(state, modifier)
    }

val npcDetailScreenRegistration =
    CircuitSerializerRegistration { it.subclass(NpcDetailScreen::class, NpcDetailScreen.serializer()) }
