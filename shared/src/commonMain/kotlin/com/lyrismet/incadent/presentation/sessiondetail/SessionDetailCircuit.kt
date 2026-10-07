package com.lyrismet.incadent.presentation.sessiondetail

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

@Suppress("LongParameterList")
fun Circuit.Builder.addSessionDetailUi(
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    partyRepository: PartyRepository,
    mentionRepositories: MentionRepositories,
    tagRepository: TagRepository,
    undoController: UndoController,
    appPreferencesRepository: AppPreferencesRepository,
): Circuit.Builder =
    addPresenter<SessionDetailScreen, SessionDetailState> { screen, navigator, _ ->
        SessionDetailPresenter(
            screen,
            navigator,
            sessionNoteRepository,
            sessionEntryRepository,
            partyRepository,
            mentionRepositories,
            tagRepository,
            undoController,
            appPreferencesRepository,
        )
    }.addUi<SessionDetailScreen, SessionDetailState> { state, modifier ->
        SessionDetailUi(state, modifier)
    }

val sessionDetailScreenRegistration =
    CircuitSerializerRegistration { it.subclass(SessionDetailScreen::class, SessionDetailScreen.serializer()) }
