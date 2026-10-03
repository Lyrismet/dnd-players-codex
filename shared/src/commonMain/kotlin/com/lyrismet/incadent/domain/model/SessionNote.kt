package com.lyrismet.incadent.domain.model

import kotlinx.datetime.LocalDateTime

data class SessionNote(
    val id: Long,
    val title: String,
    val sessionDate: LocalDateTime,
    val endedAt: LocalDateTime?,
) {
    val isLive: Boolean get() = endedAt == null
}
