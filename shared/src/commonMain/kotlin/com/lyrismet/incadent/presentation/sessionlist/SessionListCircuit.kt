package com.lyrismet.incadent.presentation.sessionlist

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

@Suppress("LongParameterList")
fun Circuit.Builder.addSessionListUi(
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    partyRepository: PartyRepository,
    mentionRepositories: MentionRepositories,
    undoController: UndoController,
    appPreferencesRepository: AppPreferencesRepository,
): Circuit.Builder =
    addPresenter<SessionListScreen, SessionListState> { _, navigator, _ ->
        SessionListPresenter(
            navigator,
            sessionNoteRepository,
            sessionEntryRepository,
            partyRepository,
            mentionRepositories,
            undoController,
            appPreferencesRepository,
        )
    }.addUi<SessionListScreen, SessionListState> { state, modifier ->
        SessionListUi(state, modifier)
    }

val sessionListScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionListScreen::class, SessionListScreen.serializer()) }
