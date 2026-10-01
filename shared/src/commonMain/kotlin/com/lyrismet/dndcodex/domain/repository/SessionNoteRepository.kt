package com.lyrismet.dndcodex.domain.repository

import com.lyrismet.dndcodex.domain.model.SessionNote
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface SessionNoteRepository {
    fun observeAll(): Flow<List<SessionNote>>

    suspend fun getById(id: Long): SessionNote?

    suspend fun upsert(sessionNote: SessionNote): Long

    suspend fun delete(id: Long)

    // enforces the "only one live session" invariant - call after a session becomes live
    suspend fun endOtherLiveSessions(
        exceptId: Long,
        endedAt: LocalDateTime,
    )
}
