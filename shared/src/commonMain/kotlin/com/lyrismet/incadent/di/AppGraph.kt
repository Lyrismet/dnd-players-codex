package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.file.ExchangeRequests
import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.LanguageRepository
import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.persistence.DatabaseDriverFactory
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
    val campaignRepository: CampaignRepository
    val undoController: UndoController
    val exchangeRequests: ExchangeRequests
    val swipeHintReplayController: SwipeHintReplayController

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides settings: ObservableSettings,
            @Provides databaseDriverFactory: DatabaseDriverFactory,
            @Provides fileStorage: FileStorage,
        ): AppGraph
    }
}
