package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.serialization.CircuitSerializerRegistration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Inject
@ContributesIntoSet(AppScope::class)
class ExportPresenterFactory(
    private val repository: CampaignExchangeRepository,
    private val storage: FileStorage,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is ExportScreen -> ExportPresenter(screen, repository, storage, navigator)
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class ExportUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is ExportScreen -> ui<ExportState> { state, modifier -> ExportUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object ExportScreenBindings {
    @Provides
    @IntoSet
    fun provideExportScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(ExportScreen::class, ExportScreen.serializer()) }
}
