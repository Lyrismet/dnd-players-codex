package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toRomanNumeral
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.lyrismet.dndcodex.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.default_campaign_name
import dndplayerscodex.shared.generated.resources.new_session_default_title
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SessionListPresenter(
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
) : Presenter<SessionListState> {
    @OptIn(ExperimentalTime::class)
    @Composable
    override fun present(): SessionListState {
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        // campaign name isn't backed by settings yet - the Settings screen will own this once it exists
        val campaignName = stringResource(Res.string.default_campaign_name)
        val newSessionTitle = stringResource(Res.string.new_session_default_title)

        return SessionListState(
            campaignName = campaignName,
            sessions = sessions.toListItems(),
        ) { event ->
            when (event) {
                SessionListEvent.NewSessionClicked ->
                    scope.launch {
                        val id =
                            sessionNoteRepository.upsert(
                                SessionNote(
                                    id = 0,
                                    title = newSessionTitle,
                                    sessionDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                                    endedAt = null,
                                ),
                            )
                        navigator.goTo(SessionDetailScreen(id))
                    }
                is SessionListEvent.SessionClicked -> navigator.goTo(SessionDetailScreen(event.id))

                is SessionListEvent.DeleteSessionClicked ->
                    scope.launch {
                        sessionNoteRepository.delete(event.id)
                    }
            }
        }
    }

    private fun List<SessionNote>.toListItems(): List<SessionListItem> =
        mapIndexed { index, note ->
            SessionListItem(
                id = note.id,
                numberLabel = (size - index).toRomanNumeral(),
                title = note.title,
                dateLabel = note.sessionDate.toDisplayDate(),
            )
        }
}
