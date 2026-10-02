package com.lyrismet.dndcodex.presentation.codex

import com.lyrismet.dndcodex.core.undo.UndoController
import com.lyrismet.dndcodex.domain.repository.LocationRepository
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.lyrismet.dndcodex.domain.repository.QuestRepository
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.CircuitSerializerRegistration

@Suppress("LongParameterList")
fun Circuit.Builder.addCodexUi(
    npcRepository: NpcRepository,
    questRepository: QuestRepository,
    locationRepository: LocationRepository,
    sessionNoteRepository: SessionNoteRepository,
    sessionEntryRepository: SessionEntryRepository,
    undoController: UndoController,
): Circuit.Builder =
    addPresenter<CodexScreen, CodexState> { _, navigator, _ ->
        CodexPresenter(
            navigator,
            npcRepository,
            questRepository,
            locationRepository,
            sessionNoteRepository,
            sessionEntryRepository,
            undoController,
        )
    }.addUi<CodexScreen, CodexState> { state, modifier ->
        CodexUi(state, modifier)
    }

val codexScreenRegistration =
    CircuitSerializerRegistration { it.subclass(CodexScreen::class, CodexScreen.serializer()) }
