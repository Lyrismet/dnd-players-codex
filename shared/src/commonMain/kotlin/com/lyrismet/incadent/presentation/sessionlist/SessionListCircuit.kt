package com.lyrismet.incadent.presentation.sessionlist

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.CodexRepositories
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.domain.repository.TagRepository
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
class SessionListPresenterFactory(
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val codexRepositories: CodexRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val campaignRepository: CampaignRepository,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is SessionListScreen ->
                SessionListPresenter(
                    navigator,
                    sessionNoteRepository,
                    sessionEntryRepository,
                    codexRepositories,
                    tagRepository,
                    undoController,
                    appPreferencesRepository,
                    campaignRepository,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class SessionListUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is SessionListScreen -> ui<SessionListState> { state, modifier -> SessionListUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object SessionListScreenBindings {
    @Provides
    @IntoSet
    fun provideSessionListScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(SessionListScreen::class, SessionListScreen.serializer()) }
}
