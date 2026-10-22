package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.file.ExchangeRequests
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
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
class ImportPresenterFactory(
    private val repository: CampaignExchangeRepository,
    private val campaigns: CampaignRepository,
    private val requests: ExchangeRequests,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is ImportScreen -> ImportPresenter(repository, campaigns, requests, navigator)
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class ImportUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is ImportScreen -> ui<ImportState> { state, modifier -> ImportUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object ImportScreenBindings {
    @Provides
    @IntoSet
    fun provideImportScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(ImportScreen::class, ImportScreen.serializer()) }
}
