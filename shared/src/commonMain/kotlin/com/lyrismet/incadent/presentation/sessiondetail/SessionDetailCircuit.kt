package com.lyrismet.incadent.presentation.sessiondetail

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

fun Circuit.Builder.addSessionDetailUi(
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    mentionRepositories: MentionRepositories,
    undoController: UndoController,
): Circuit.Builder =
    addPresenter<SessionDetailScreen, SessionDetailState> { screen, navigator, _ ->
        SessionDetailPresenter(
            screen,
            navigator,
            sessionNoteRepository,
            sessionEntryRepository,
            mentionRepositories,
            undoController,
        )
    }.addUi<SessionDetailScreen, SessionDetailState> { state, modifier ->
        SessionDetailUi(state, modifier)
    }

val sessionDetailScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionDetailScreen::class, SessionDetailScreen.serializer()) }
