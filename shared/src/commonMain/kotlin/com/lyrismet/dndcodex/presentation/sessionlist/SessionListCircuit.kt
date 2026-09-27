package com.lyrismet.dndcodex.presentation.sessionlist

import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addSessionListUi(
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    mentionRepositories: MentionRepositories,
): Circuit.Builder =
    addPresenter<SessionListScreen, SessionListState> { _, navigator, _ ->
        SessionListPresenter(
            navigator,
            sessionNoteRepository,
            sessionEntryRepository,
            mentionRepositories,
        )
    }.addUi<SessionListScreen, SessionListState> { state, modifier ->
        SessionListUi(state, modifier)
    }

val sessionListScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionListScreen::class, SessionListScreen.serializer()) }
