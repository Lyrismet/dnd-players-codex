package com.lyrismet.dndcodex.presentation.sessionlist

import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addSessionListUi(sessionNoteRepository: SessionNoteRepository): Circuit.Builder =
    addPresenter<SessionListScreen, SessionListState> { _, _, _ ->
        SessionListPresenter(sessionNoteRepository)
    }.addUi<SessionListScreen, SessionListState> { state, modifier ->
        SessionListUi(state, modifier)
    }

val sessionListScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionListScreen::class, SessionListScreen.serializer()) }
