package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toRomanNumeral
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// campaign name isn't backed by settings yet - the Settings screen will own this once it exists
private const val PLACEHOLDER_CAMPAIGN_NAME = "Моя кампания"

// takes a Navigator once session detail exists to navigate to on SessionClicked
class SessionListPresenter(
    private val sessionNoteRepository: SessionNoteRepository,
) : Presenter<SessionListState> {
    @OptIn(ExperimentalTime::class)
    @Composable
    override fun present(): SessionListState {
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        return SessionListState(
            campaignName = PLACEHOLDER_CAMPAIGN_NAME,
            sessions = sessions.toListItems(),
        ) { event ->
            when (event) {
                SessionListEvent.NewSessionClicked ->
                    scope.launch {
                        sessionNoteRepository.upsert(
                            SessionNote(
                                id = 0,
                                title = "Новая сессия",
                                content = "",
                                sessionDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                                mentionedNpcIds = emptyList(),
                                mentionedQuestIds = emptyList(),
                                mentionedLocationIds = emptyList(),
                            ),
                        )
                    }
                // session detail screen doesn't exist yet - nothing to navigate to
                is SessionListEvent.SessionClicked -> Unit
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
                hasMentions =
                    note.mentionedNpcIds.isNotEmpty() ||
                        note.mentionedQuestIds.isNotEmpty() ||
                        note.mentionedLocationIds.isNotEmpty(),
            )
        }
}
