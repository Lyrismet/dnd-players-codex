package com.lyrismet.incadent.feature.combat.presentation.results

import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
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
class CombatResultsPresenterFactory(
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val hitObservationRepository: HitObservationRepository,
    private val codexRepositories: CodexRepositories,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val undoController: UndoController,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is CombatResultsScreen ->
                CombatResultsPresenter(
                    screen,
                    navigator,
                    combatRepository,
                    sessionNoteRepository,
                    hitObservationRepository,
                    codexRepositories,
                    appPreferencesRepository,
                    undoController,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class CombatResultsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is CombatResultsScreen -> ui<CombatResultsState> { state, modifier -> CombatResultsUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object CombatResultsScreenBindings {
    @Provides
    @IntoSet
    fun provideCombatResultsScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(CombatResultsScreen::class, CombatResultsScreen.serializer()) }
}
