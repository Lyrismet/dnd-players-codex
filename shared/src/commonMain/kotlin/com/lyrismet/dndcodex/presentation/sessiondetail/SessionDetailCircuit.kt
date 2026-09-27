package com.lyrismet.dndcodex.presentation.sessiondetail

import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addSessionDetailUi(
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    mentionRepositories: MentionRepositories,
): Circuit.Builder =
    addPresenter<SessionDetailScreen, SessionDetailState> { screen, navigator, _ ->
        SessionDetailPresenter(
            screen,
            navigator,
            sessionNoteRepository,
            sessionEntryRepository,
            mentionRepositories,
        )
    }.addUi<SessionDetailScreen, SessionDetailState> { state, modifier ->
        SessionDetailUi(state, modifier)
    }

val sessionDetailScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionDetailScreen::class, SessionDetailScreen.serializer()) }
