package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.format.toDisplayTime
import com.lyrismet.dndcodex.core.format.toRomanNumeral
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_overline_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SessionDetailPresenter(
    private val screen: SessionDetailScreen,
    private val navigator: Navigator,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
) : Presenter<SessionDetailState> {
    @OptIn(ExperimentalTime::class)
    @Composable
    override fun present(): SessionDetailState {
        val allSessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val entries by sessionEntryRepository
            .observeForSession(screen.sessionNoteId)
            .collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        val currentNote = allSessions.find { it.id == screen.sessionNoteId }
        val numberLabel = numberLabelFor(allSessions)

        // edited locally so live Flow re-emissions from other screens don't clobber in-progress typing
        val titleField = remember(screen.sessionNoteId) { mutableStateOf<String?>(null) }
        val draft = remember(screen.sessionNoteId) { mutableStateOf("") }

        LaunchedEffect(currentNote?.id) {
            if (titleField.value == null && currentNote != null) {
                titleField.value = currentNote.title
            }
        }

        // deliberately renders the same header/feed/composer shape whether or not currentNote
        // has arrived yet from the Flow's cold start - swapping to a distinct "loading" screen
        // for that one frame was the cause of a visible flash on every navigation into this screen
        return SessionDetailState(
            isLoading = currentNote == null,
            overline = numberLabel?.let { stringResource(Res.string.session_overline_format, it) }.orEmpty(),
            title = titleField.value ?: currentNote?.title.orEmpty(),
            dateLabel = currentNote?.sessionDate?.toDisplayDate().orEmpty(),
            isLive = currentNote?.isLive ?: true,
            entries = entries.map { SessionEntryItem(it.id, it.createdAt.toDisplayTime(), it.body) },
            draft = draft.value,
        ) { event -> onEvent(event, currentNote, scope, titleField, draft) }
    }

    private fun numberLabelFor(allSessions: List<SessionNote>): String? =
        allSessions
            .sortedBy { it.sessionDate }
            .indexOfFirst { it.id == screen.sessionNoteId }
            .takeIf { it >= 0 }
            ?.let { (it + 1).toRomanNumeral() }

    @OptIn(ExperimentalTime::class)
    private fun onEvent(
        event: SessionDetailEvent,
        currentNote: SessionNote?,
        scope: CoroutineScope,
        titleField: MutableState<String?>,
        draft: MutableState<String>,
    ) {
        when (event) {
            SessionDetailEvent.BackClicked -> navigator.pop()

            is SessionDetailEvent.TitleChanged -> {
                titleField.value = event.title
                currentNote?.let { note ->
                    scope.launch { sessionNoteRepository.upsert(note.copy(title = event.title)) }
                }
            }

            is SessionDetailEvent.DraftChanged -> draft.value = event.text

            SessionDetailEvent.SubmitEntryClicked -> {
                val body = draft.value.trim()
                if (body.isNotEmpty()) {
                    draft.value = ""
                    scope.launch { sessionEntryRepository.add(screen.sessionNoteId, body) }
                }
            }

            is SessionDetailEvent.DeleteEntryClicked ->
                scope.launch {
                    sessionEntryRepository.delete(event.id)
                }

            SessionDetailEvent.EndSessionClicked ->
                currentNote?.let { note ->
                    scope.launch {
                        val endedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        sessionNoteRepository.upsert(note.copy(endedAt = endedAt))
                    }
                }

            SessionDetailEvent.ResumeSessionClicked ->
                currentNote?.let { note ->
                    scope.launch { sessionNoteRepository.upsert(note.copy(endedAt = null)) }
                }
        }
    }
}
