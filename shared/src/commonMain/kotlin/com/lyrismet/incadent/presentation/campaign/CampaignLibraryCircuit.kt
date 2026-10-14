package com.lyrismet.incadent.presentation.campaign

import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
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
class CampaignLibraryPresenterFactory(
    private val campaignRepository: CampaignRepository,
    private val languageRepository: LanguageRepository,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is CampaignLibraryScreen -> CampaignLibraryPresenter(campaignRepository, languageRepository)
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class CampaignLibraryUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is CampaignLibraryScreen ->
                ui<CampaignLibraryScreenState> { state, modifier -> CampaignLibraryUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object CampaignLibraryScreenBindings {
    @Provides
    @IntoSet
    fun provideCampaignLibraryScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(CampaignLibraryScreen::class, CampaignLibraryScreen.serializer()) }
}
