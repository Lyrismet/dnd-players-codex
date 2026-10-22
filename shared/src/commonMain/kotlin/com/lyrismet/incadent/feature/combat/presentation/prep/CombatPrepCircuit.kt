package com.lyrismet.incadent.feature.combat.presentation.prep

import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
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
class CombatPrepPresenterFactory(
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val codexRepositories: CodexRepositories,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is CombatPrepScreen ->
                CombatPrepPresenter(
                    screen.combatId,
                    navigator,
                    combatRepository,
                    sessionNoteRepository,
                    sessionEntryRepository,
                    codexRepositories,
                    appPreferencesRepository,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class CombatPrepUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is CombatPrepScreen -> ui<CombatPrepState> { state, modifier -> CombatPrepUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object CombatPrepScreenBindings {
    @Provides
    @IntoSet
    fun provideCombatPrepScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(CombatPrepScreen::class, CombatPrepScreen.serializer()) }
}
