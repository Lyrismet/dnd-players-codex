package com.lyrismet.incadent.feature.session.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
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
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : SessionNoteRepository {
    private val queries = database.sessionNoteQueries

    override fun observeAll(): Flow<List<SessionNote>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(Dispatchers.Default)
        }

    override suspend fun getById(id: Long): SessionNote? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun upsert(sessionNote: SessionNote): Long =
        withContext(Dispatchers.Default) {
            if (sessionNote.id == 0L) {
                database.transactionWithResult {
                    queries.insert(
                        sessionNote.title,
                        sessionNote.sessionDate,
                        sessionNote.endedAt,
                        Clock.System.now().toEpochMilliseconds(),
                        current.requireId(),
                    )
                    queries.lastInsertRowId().executeAsOne()
                }
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

    override suspend fun makeOnlyLive(
        id: Long,
        now: LocalDateTime,
    ) {
        withContext(Dispatchers.Default) {
            database.transaction {
                queries.reopen(id)
                queries.endOtherLive(now, id, current.requireId())
            }
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
