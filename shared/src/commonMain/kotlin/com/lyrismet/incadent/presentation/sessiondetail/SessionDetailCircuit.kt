package com.lyrismet.incadent.presentation.sessiondetail

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.MentionRepositories
import com.lyrismet.incadent.domain.repository.PartyRepository
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
class SessionDetailPresenterFactory(
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val partyRepository: PartyRepository,
    private val mentionRepositories: MentionRepositories,
    private val tagRepository: TagRepository,
    private val undoController: UndoController,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            is SessionDetailScreen ->
                SessionDetailPresenter(
                    screen,
                    navigator,
                    sessionNoteRepository,
                    sessionEntryRepository,
                    partyRepository,
                    mentionRepositories,
                    tagRepository,
                    undoController,
                    appPreferencesRepository,
                )
            else -> null
        }
}

@Inject
@ContributesIntoSet(AppScope::class)
class SessionDetailUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? =
        when (screen) {
            is SessionDetailScreen -> ui<SessionDetailState> { state, modifier -> SessionDetailUi(state, modifier) }
            else -> null
        }
}

@BindingContainer
@ContributesTo(AppScope::class)
object SessionDetailScreenBindings {
    @Provides
    @IntoSet
    fun provideSessionDetailScreenRegistration(): CircuitSerializerRegistration =
        CircuitSerializerRegistration { it.subclass(SessionDetailScreen::class, SessionDetailScreen.serializer()) }
}
