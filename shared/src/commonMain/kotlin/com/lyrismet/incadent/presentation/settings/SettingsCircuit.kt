package com.lyrismet.incadent.presentation.settings

import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
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
class SettingsPresenterFactory(
    private val languageRepository: LanguageRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val swipeHintReplayController: SwipeHintReplayController,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is SettingsScreen ->
                SettingsPresenter(
                    languageRepository,
                    appPreferencesRepository,
                    swipeHintReplayController,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class SettingsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is SettingsScreen -> ui<SettingsState> { state, modifier -> SettingsUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object SettingsScreenBindings {
    @Provides
    @IntoSet
    fun provideSettingsScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(SettingsScreen::class, SettingsScreen.serializer()) }
}
