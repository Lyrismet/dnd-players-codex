package com.lyrismet.incadent.feature.session.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.newOriginId
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : SessionEntryRepository {
    private val queries = database.sessionEntryQueries

    override fun observeForSession(sessionNoteId: Long): Flow<List<SessionEntry>> =
        queries.selectForSession(sessionNoteId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)

    override fun observeAll(): Flow<List<SessionEntry>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun add(
        sessionNoteId: Long,
        body: String,
    ): Long =
        withContext(DatabaseDispatcher) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            database.transactionWithResult {
                queries.insert(
                    sessionNoteId,
                    now,
                    body,
                    edited = false,
                    mention_check = true,
                    origin_id = newOriginId(),
                )
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun restore(entry: SessionEntry): Long =
        withContext(DatabaseDispatcher) {
            queries.insertWithId(
                entry.id,
                entry.sessionNoteId,
                entry.createdAt,
                entry.body,
                entry.edited,
                entry.mentionCheck,
                entry.originId.orNewOriginId(),
            )
            entry.id
        }

    override suspend fun update(
        id: Long,
        body: String,
    ) {
        withContext(DatabaseDispatcher) {
            queries.updateBody(body, id)
        }
    }

    override suspend fun rewriteBody(
        id: Long,
        body: String,
    ) {
        withContext(DatabaseDispatcher) { queries.renameBody(body, id) }
    }

    override suspend fun setMentionCheck(
        id: Long,
        check: Boolean,
    ) {
        withContext(DatabaseDispatcher) { queries.setMentionCheck(check, id) }
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) {
            queries.deleteById(id)
        }
    }

    // one parameter per column of the row, the shape SQLDelight hands a mapper
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        sessionNoteId: Long,
        createdAt: LocalDateTime,
        body: String,
        edited: Boolean,
        mentionCheck: Boolean,
        originId: String,
    ) = SessionEntry(
        id = id,
        sessionNoteId = sessionNoteId,
        createdAt = createdAt,
        body = body,
        edited = edited,
        mentionCheck = mentionCheck,
        originId = originId,
    )
}
