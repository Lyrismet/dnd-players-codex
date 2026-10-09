package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
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
class CodexPresenterFactory(
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val imageCompressor: ImageCompressor,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is CodexScreen ->
                CodexPresenter(
                    navigator,
                    npcRepository,
                    partyRepository,
                    questRepository,
                    locationRepository,
                    sessionNoteRepository,
                    sessionEntryRepository,
                    undoController,
                    appPreferencesRepository,
                    imageCompressor,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class CodexUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is CodexScreen -> ui<CodexState> { state, modifier -> CodexUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object CodexScreenBindings {
    @Provides
    @IntoSet
    fun provideCodexScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(CodexScreen::class, CodexScreen.serializer()) }
}
