package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
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
            queries.insert(sessionNoteId, now, body, edited = 0L)
            queries.lastInsertRowId().executeAsOne()
        }

    override suspend fun restore(entry: SessionEntry): Long =
        withContext(Dispatchers.Default) {
            queries.insert(entry.sessionNoteId, entry.createdAt, entry.body, if (entry.edited) 1L else 0L)
            queries.lastInsertRowId().executeAsOne()
        }

    override suspend fun update(
        id: Long,
        body: String,
    ) {
        withContext(Dispatchers.Default) {
            queries.updateBody(body, id)
        }
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
        edited: Long,
    ) = SessionEntry(id = id, sessionNoteId = sessionNoteId, createdAt = createdAt, body = body, edited = edited != 0L)
}
