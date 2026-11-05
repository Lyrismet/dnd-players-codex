package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
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
class MePresenterFactory(
    private val partyRepository: PartyRepository,
    private val sheetRepository: CharacterSheetRepository,
    private val resourceRepository: CharacterResourceRepository,
    private val campaignRepository: CampaignRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is MeScreen ->
                MePresenter(
                    partyRepository,
                    sheetRepository,
                    resourceRepository,
                    campaignRepository,
                    undoController,
                    appPreferencesRepository,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class MeUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is MeScreen -> ui<MeState> { state, modifier -> MeUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object MeScreenBindings {
    @Provides
    @IntoSet
    fun provideMeScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(MeScreen::class, MeScreen.serializer()) }
}
