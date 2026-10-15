package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.SessionNote
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface SessionNoteRepository {
    fun observeAll(): Flow<List<SessionNote>>

    suspend fun getById(id: Long): SessionNote?

    suspend fun upsert(sessionNote: SessionNote): Long

    suspend fun delete(id: Long)

    /** [id] becomes the campaign's only live session - reopened if ended, every other live one ends at [now] */
    suspend fun makeOnlyLive(
        id: Long,
        now: LocalDateTime,
    )
}
