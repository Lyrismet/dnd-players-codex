package com.lyrismet.incadent.feature.combat.presentation.list

import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
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
@Suppress("LongParameterList")
class CombatListPresenterFactory(
    private val combatRepository: CombatRepository,
    private val codexRepositories: CodexRepositories,
    private val sessionNoteRepository: SessionNoteRepository,
    private val campaignRepository: CampaignRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val importSource: ImportSource,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is CombatListScreen ->
                CombatListPresenter(
                    navigator,
                    combatRepository,
                    codexRepositories,
                    sessionNoteRepository,
                    campaignRepository,
                    appPreferencesRepository,
                    importSource,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class CombatListUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is CombatListScreen -> ui<CombatListState> { state, modifier -> CombatListUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object CombatListScreenBindings {
    @Provides
    @IntoSet
    fun provideCombatListScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(CombatListScreen::class, CombatListScreen.serializer()) }
}
