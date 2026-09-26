package com.lyrismet.dndcodex.domain.model

import kotlinx.datetime.LocalDateTime

/** one timestamped note in a session's feed - text only for now, combat/audio entries come later */
data class SessionEntry(
    val id: Long,
    val sessionNoteId: Long,
    val createdAt: LocalDateTime,
    val body: String,
)
