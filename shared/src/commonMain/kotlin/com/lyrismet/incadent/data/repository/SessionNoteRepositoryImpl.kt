package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.repository.SessionNoteRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SessionNoteRepositoryImpl(
    database: AppDatabase,
) : SessionNoteRepository {
    private val queries = database.sessionNoteQueries

    override fun observeAll(): Flow<List<SessionNote>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): SessionNote? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun upsert(sessionNote: SessionNote): Long =
        withContext(Dispatchers.Default) {
            if (sessionNote.id == 0L) {
                queries.insert(
                    sessionNote.title,
                    sessionNote.sessionDate,
                    sessionNote.endedAt,
                    Clock.System.now().toEpochMilliseconds(),
                )
                queries.lastInsertRowId().executeAsOne()
            } else {
                queries.update(sessionNote.title, sessionNote.sessionDate, sessionNote.endedAt, sessionNote.id)
                sessionNote.id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    override suspend fun endOtherLiveSessions(
        exceptId: Long,
        endedAt: LocalDateTime,
    ) {
        withContext(Dispatchers.Default) {
            queries.endOtherLive(endedAt, exceptId)
        }
    }

    // createdAt is required to match selectAll's column order even though the domain model doesn't expose it
    @Suppress("UnusedParameter")
    private fun toDomain(
        id: Long,
        title: String,
        sessionDate: LocalDateTime,
        endedAt: LocalDateTime?,
        createdAt: Long,
    ) = SessionNote(id = id, title = title, sessionDate = sessionDate, endedAt = endedAt)
}
