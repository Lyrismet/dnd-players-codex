package com.lyrismet.dndcodex.domain.model

import kotlinx.datetime.LocalDateTime

data class SessionNote(
    val id: Long,
    val title: String,
    val content: String,
    val sessionDate: LocalDateTime,
    val mentionedNpcIds: List<Long>,
    val mentionedQuestIds: List<Long>,
    val mentionedLocationIds: List<Long>,
)
