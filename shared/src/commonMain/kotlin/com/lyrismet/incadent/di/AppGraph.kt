package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
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
    val languageRepository: LanguageRepository
    val appPreferencesRepository: AppPreferencesRepository
    val swipeHintReplayController: SwipeHintReplayController

    val presenterFactories: Set<Presenter.Factory>
    val uiFactories: Set<Ui.Factory>
    val screenRegistrations: Set<CircuitSerializerRegistration>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides settings: ObservableSettings,
        ): AppGraph
    }
}
