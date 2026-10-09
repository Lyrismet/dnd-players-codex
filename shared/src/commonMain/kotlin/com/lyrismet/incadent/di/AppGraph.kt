package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.russhwolf.settings.ObservableSettings
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.serialization.CircuitSerializerRegistration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/** app-wide Metro graph - features migrated off AppContainer contribute bindings and Circuit factories here */
@DependencyGraph(AppScope::class)
interface AppGraph {
    // exposed only for what AppContainer still wires by hand (codex) and for App - drop with AppContainer
    val languageRepository: LanguageRepository
    val appPreferencesRepository: AppPreferencesRepository
    val swipeHintReplayController: SwipeHintReplayController
    val undoController: UndoController
    val imageCompressor: ImageCompressor
    val sessionNoteRepository: SessionNoteRepository
    val sessionEntryRepository: SessionEntryRepository
    val partyRepository: PartyRepository
    val npcRepository: NpcRepository
    val questRepository: QuestRepository
    val locationRepository: LocationRepository

    val presenterFactories: Set<Presenter.Factory>
    val uiFactories: Set<Ui.Factory>
    val screenRegistrations: Set<CircuitSerializerRegistration>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides settings: ObservableSettings,
            @Provides databaseDriverFactory: DatabaseDriverFactory,
        ): AppGraph
    }
}
