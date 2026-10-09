package com.lyrismet.incadent.data

import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.settings.SettingsFactory
import com.lyrismet.incadent.di.AppGraph
import com.lyrismet.incadent.presentation.codex.addCodexUi
import com.lyrismet.incadent.presentation.codex.codexScreenRegistration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.SerializableCircuitSaver
import dev.zacsweers.metro.createGraphFactory

/** legacy shell around [AppGraph] - only codex is still wired by hand, delete this once it moves to Metro */
class AppContainer(
    databaseDriverFactory: DatabaseDriverFactory,
    settingsFactory: SettingsFactory,
) {
    private val graph =
        createGraphFactory<AppGraph.Factory>().create(settingsFactory.createSettings(), databaseDriverFactory)

    val languageRepository = graph.languageRepository
    val appPreferencesRepository = graph.appPreferencesRepository
    val undoController = graph.undoController
    val swipeHintReplayController = graph.swipeHintReplayController

    val circuit: Circuit =
        Circuit
            .Builder()
            .addPresenterFactories(graph.presenterFactories)
            .addUiFactories(graph.uiFactories)
            // кодекс
            .addCodexUi(
                graph.npcRepository,
                graph.partyRepository,
                graph.questRepository,
                graph.locationRepository,
                graph.sessionNoteRepository,
                graph.sessionEntryRepository,
                graph.undoController,
                graph.appPreferencesRepository,
                graph.imageCompressor,
            ).setCircuitSaver(
                SerializableCircuitSaver(listOf(codexScreenRegistration) + graph.screenRegistrations),
            ).build()
}
