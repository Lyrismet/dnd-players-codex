package com.lyrismet.incadent.feature.session.presentation.route

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** what an entity sheet calls to open a session at one of its notes, an entry id of 0 opens the session alone */
fun Navigator.sessionEntryOpener(): (Long, Long) -> Unit =
    { sessionNoteId, entryId -> goTo(SessionDetailScreen(sessionNoteId, focusEntryId = entryId.takeIf { it > 0 })) }

@Serializable
data class SessionDetailScreen(
    val sessionNoteId: Long,
    // set when opened from a related note - the entry is scrolled to and highlighted
    val focusEntryId: Long? = null,
) : Screen
