package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.russhwolf.settings.ObservableSettings
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/** app-wide Metro graph - the accessors are only what the App composition root reads, features inject the rest */
@DependencyGraph(AppScope::class)
interface AppGraph {
    val circuit: Circuit
    val languageRepository: LanguageRepository
    val appPreferencesRepository: AppPreferencesRepository
    val undoController: UndoController
    val swipeHintReplayController: SwipeHintReplayController

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides settings: ObservableSettings,
            @Provides databaseDriverFactory: DatabaseDriverFactory,
        ): AppGraph
    }
}
