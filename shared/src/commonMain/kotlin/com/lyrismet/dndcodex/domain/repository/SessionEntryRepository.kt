package com.lyrismet.dndcodex.domain.repository

import com.lyrismet.dndcodex.domain.model.SessionEntry
import kotlinx.coroutines.flow.Flow

interface SessionEntryRepository {
    fun observeForSession(sessionNoteId: Long): Flow<List<SessionEntry>>

    suspend fun add(
        sessionNoteId: Long,
        body: String,
    ): Long

    suspend fun delete(id: Long)
}
