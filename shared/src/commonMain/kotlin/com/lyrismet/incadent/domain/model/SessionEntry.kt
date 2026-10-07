package com.lyrismet.incadent.domain.model

import kotlinx.datetime.LocalDateTime

/** one timestamped note in a session's feed - text only for now, combat/audio entries come later */
data class SessionEntry(
    val id: Long,
    val sessionNoteId: Long,
    val createdAt: LocalDateTime,
    val body: String,
    // set once the note has been edited after it was sent, shown as "изм." in the feed
    val edited: Boolean = false,
)
