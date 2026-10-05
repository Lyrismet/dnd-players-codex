package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

@Suppress("LongParameterList")
fun Circuit.Builder.addCodexUi(
    npcRepository: NpcRepository,
    partyRepository: PartyRepository,
    questRepository: QuestRepository,
    locationRepository: LocationRepository,
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    undoController: UndoController,
    appPreferencesRepository: AppPreferencesRepository,
): Circuit.Builder =
    addPresenter<CodexScreen, CodexState> { _, navigator, _ ->
        CodexPresenter(
            navigator,
            npcRepository,
            partyRepository,
            questRepository,
            locationRepository,
            sessionNoteRepository,
            sessionEntryRepository,
            undoController,
            appPreferencesRepository,
        )
    }.addUi<CodexScreen, CodexState> { state, modifier ->
        CodexUi(state, modifier)
    }

val codexScreenRegistration =
    CircuitSerializerRegistration { it.subclass(CodexScreen::class, CodexScreen.serializer()) }
