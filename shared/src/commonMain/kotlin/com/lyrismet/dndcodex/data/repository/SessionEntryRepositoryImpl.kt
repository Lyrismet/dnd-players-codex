package com.lyrismet.dndcodex.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.dndcodex.db.AppDatabase
import com.lyrismet.dndcodex.domain.model.SessionEntry
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SessionEntryRepositoryImpl(
    database: AppDatabase,
) : SessionEntryRepository {
    private val queries = database.sessionEntryQueries

    override fun observeForSession(sessionNoteId: Long): Flow<List<SessionEntry>> =
        queries.selectForSession(sessionNoteId, ::toDomain).asFlow().mapToList(Dispatchers.Default)

    override fun observeAll(): Flow<List<SessionEntry>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    @OptIn(ExperimentalTime::class)
    override suspend fun add(
        sessionNoteId: Long,
        body: String,
    ): Long =
        withContext(Dispatchers.Default) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            queries.insert(sessionNoteId, now, body)
            queries.lastInsertRowId().executeAsOne()
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    private fun toDomain(
        id: Long,
        sessionNoteId: Long,
        createdAt: LocalDateTime,
        body: String,
    ) = SessionEntry(id = id, sessionNoteId = sessionNoteId, createdAt = createdAt, body = body)
}
