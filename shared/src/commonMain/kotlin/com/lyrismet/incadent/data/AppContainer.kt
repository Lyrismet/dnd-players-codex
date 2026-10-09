package com.lyrismet.incadent.data

import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.db.createAppDatabase
import com.lyrismet.incadent.data.repository.LocationRepositoryImpl
import com.lyrismet.incadent.data.repository.NpcRepositoryImpl
import com.lyrismet.incadent.data.repository.PartyRepositoryImpl
import com.lyrismet.incadent.data.repository.QuestRepositoryImpl
import com.lyrismet.incadent.data.repository.SessionEntryRepositoryImpl
import com.lyrismet.incadent.data.repository.SessionNoteRepositoryImpl
import com.lyrismet.incadent.data.repository.TagRepositoryImpl
import com.lyrismet.incadent.data.settings.SettingsFactory
import com.lyrismet.incadent.di.AppGraph
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
import com.lyrismet.incadent.presentation.codex.addCodexUi
import com.lyrismet.incadent.presentation.codex.codexScreenRegistration
import com.lyrismet.incadent.presentation.sessiondetail.addSessionDetailUi
import com.lyrismet.incadent.presentation.sessiondetail.sessionDetailScreenRegistration
import com.lyrismet.incadent.presentation.sessionlist.addSessionListUi
import com.lyrismet.incadent.presentation.sessionlist.sessionListScreenRegistration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.SerializableCircuitSaver
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    databaseDriverFactory: DatabaseDriverFactory,
    settingsFactory: SettingsFactory,
) {
    private val database = createAppDatabase(databaseDriverFactory.createDriver())

    // сессии
    val sessionNoteRepository: SessionNoteRepository = SessionNoteRepositoryImpl(database)
    val sessionEntryRepository: SessionEntryRepository = SessionEntryRepositoryImpl(database)
    val tagRepository: TagRepository = TagRepositoryImpl(database)

    // кодекс
    val npcRepository: NpcRepository = NpcRepositoryImpl(database)
    val questRepository: QuestRepository = QuestRepositoryImpl(database)
    val partyRepository: PartyRepository = PartyRepositoryImpl(database)
    val locationRepository: LocationRepository = LocationRepositoryImpl(database)
    private val mentionRepositories = MentionRepositories(npcRepository, locationRepository, questRepository)
    private val imageCompressor = ImageCompressor()

    // инфраструктура
    private val graph = createGraphFactory<AppGraph.Factory>().create(settingsFactory.createSettings())
    val languageRepository: LanguageRepository = graph.languageRepository
    val appPreferencesRepository: AppPreferencesRepository = graph.appPreferencesRepository
    val swipeHintReplayController = graph.swipeHintReplayController
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val undoController = UndoController(appScope)

    val circuit: Circuit =
        Circuit
            .Builder()
            // сессии
            .addSessionListUi(
                sessionNoteRepository,
                sessionEntryRepository,
                partyRepository,
                mentionRepositories,
                tagRepository,
                undoController,
                appPreferencesRepository,
            ).addSessionDetailUi(
                sessionNoteRepository,
                sessionEntryRepository,
                partyRepository,
                mentionRepositories,
                tagRepository,
                undoController,
                appPreferencesRepository,
            )
            // кодекс
            .addCodexUi(
                npcRepository,
                partyRepository,
                questRepository,
                locationRepository,
                sessionNoteRepository,
                sessionEntryRepository,
                undoController,
                appPreferencesRepository,
                imageCompressor,
            )
            // migrated to Metro (settings)
            .addPresenterFactories(graph.presenterFactories)
            .addUiFactories(graph.uiFactories)
            .setCircuitSaver(
                SerializableCircuitSaver(
                    listOf(
                        sessionListScreenRegistration,
                        sessionDetailScreenRegistration,
                        codexScreenRegistration,
                    ) + graph.screenRegistrations,
                ),
            ).build()
}
