package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.moveChanges
import com.lyrismet.incadent.feature.session.domain.placeAmong
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.new_session_default_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** the edit mode of the session list - which session is new, adding a past one and moving one to another place */
internal class SessionOrderHandler(
    private val editing: MutableState<Boolean>,
    private val newSessionId: MutableState<Long?>,
    private val repository: SessionNoteRepository,
    private val readOnly: Boolean,
) {
    fun onEvent(
        scope: CoroutineScope,
        event: SessionOrderEvent,
        sessions: List<SessionNote>,
    ) {
        if (readOnly) return
        when (event) {
            SessionOrderEvent.EditToggled -> {
                editing.value = !editing.value
                newSessionId.value = null
            }
            SessionOrderEvent.PastSessionAdded -> scope.launch { addPast(sessions) }
            is SessionOrderEvent.SessionMoved -> scope.launch { move(event.id, event.toIndex, sessions) }
        }
    }

    // a new session just before the current one, dated between it and the one before
    private suspend fun addPast(sessions: List<SessionNote>) {
        val live = sessions.firstOrNull { it.isLive } ?: return
        val past = sessions.filter { !it.isLive }.sortedBy { it.sessionDate }
        // just before the current session when it is the later one, else a day after the last past one
        val liveIsNewest = past.lastOrNull()?.let { it.sessionDate < live.sessionDate } ?: true
        val others = if (liveIsNewest) past + live else past
        val placement = placeAmong(others, past.size)
        val id =
            repository.upsert(
                SessionNote(
                    0,
                    getString(Res.string.new_session_default_title),
                    placement.date,
                    endedAt = placement.date,
                ),
            )
        repository.setDates(placement.shifted)
        newSessionId.value = id
    }

    // [toIndex] counts from the newest of the past sessions, as the list shows them
    private suspend fun move(
        id: Long,
        toIndex: Int,
        sessions: List<SessionNote>,
    ) {
        // the current session stays on top whatever its date, so only the past ones are ordered
        val past = sessions.filter { !it.isLive }
        val changes = past.moveChanges(id, past.size - 1 - toIndex)
        if (changes.isEmpty()) return
        repository.setDates(changes.associate { it.id to it.to })
        if (newSessionId.value == id) newSessionId.value = null
    }
}
