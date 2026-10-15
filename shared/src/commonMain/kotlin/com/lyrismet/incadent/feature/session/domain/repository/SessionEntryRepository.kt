package com.lyrismet.incadent.feature.session.domain.repository

import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import kotlinx.coroutines.flow.Flow

interface SessionEntryRepository {
    fun observeForSession(sessionNoteId: Long): Flow<List<SessionEntry>>

    fun observeAll(): Flow<List<SessionEntry>>

    suspend fun add(
        sessionNoteId: Long,
        body: String,
    ): Long

    // re-inserts a deleted entry with its original timestamp, for undo - a fresh id, same position in the feed
    suspend fun restore(entry: SessionEntry): Long

    suspend fun update(
        id: Long,
        body: String,
    )

    /** rewrites the text without marking the note edited or asking to check it again - mention links and unlinks */
    suspend fun rewriteBody(
        id: Long,
        body: String,
    )

    suspend fun setMentionCheck(
        id: Long,
        check: Boolean,
    )

    suspend fun delete(id: Long)
}
