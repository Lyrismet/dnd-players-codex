package com.lyrismet.dndcodex.data

import com.lyrismet.dndcodex.core.undo.UndoController
import com.lyrismet.dndcodex.data.db.DatabaseDriverFactory
import com.lyrismet.dndcodex.data.db.createAppDatabase
import com.lyrismet.dndcodex.data.repository.LanguageRepositoryImpl
import com.lyrismet.dndcodex.data.repository.LocationRepositoryImpl
import com.lyrismet.dndcodex.data.repository.NpcRepositoryImpl
import com.lyrismet.dndcodex.data.repository.QuestRepositoryImpl
import com.lyrismet.dndcodex.data.repository.SessionEntryRepositoryImpl
import com.lyrismet.dndcodex.data.repository.SessionNoteRepositoryImpl
import com.lyrismet.dndcodex.data.settings.SettingsFactory
import com.lyrismet.dndcodex.domain.repository.LanguageRepository
import com.lyrismet.dndcodex.domain.repository.LocationRepository
import com.lyrismet.dndcodex.domain.repository.MentionRepositories
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.lyrismet.dndcodex.domain.repository.QuestRepository
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.lyrismet.dndcodex.presentation.codex.addCodexUi
import com.lyrismet.dndcodex.presentation.codex.codexScreenRegistration
import com.lyrismet.dndcodex.presentation.sessiondetail.addSessionDetailUi
import com.lyrismet.dndcodex.presentation.sessiondetail.sessionDetailScreenRegistration
import com.lyrismet.dndcodex.presentation.sessionlist.addSessionListUi
import com.lyrismet.dndcodex.presentation.sessionlist.sessionListScreenRegistration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.SerializableCircuitSaver
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

    // кодекс
    val npcRepository: NpcRepository = NpcRepositoryImpl(database)
    val questRepository: QuestRepository = QuestRepositoryImpl(database)
    val locationRepository: LocationRepository = LocationRepositoryImpl(database)
    private val mentionRepositories = MentionRepositories(npcRepository, locationRepository, questRepository)

    // инфраструктура
    val languageRepository: LanguageRepository = LanguageRepositoryImpl(settingsFactory.createSettings())
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val undoController = UndoController(appScope)

    val circuit: Circuit =
        Circuit
            .Builder()
            // сессии
            .addSessionListUi(sessionNoteRepository, sessionEntryRepository, mentionRepositories, undoController)
            .addSessionDetailUi(sessionNoteRepository, sessionEntryRepository, mentionRepositories, undoController)
            // кодекс
            .addCodexUi(
                npcRepository,
                questRepository,
                locationRepository,
                sessionNoteRepository,
                sessionEntryRepository,
                undoController,
            ).setCircuitSaver(
                SerializableCircuitSaver(
                    listOf(
                        sessionListScreenRegistration,
                        sessionDetailScreenRegistration,
                        codexScreenRegistration,
                    ),
                ),
            ).build()
}
