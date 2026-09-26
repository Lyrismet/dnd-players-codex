package com.lyrismet.dndcodex.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.dndcodex.db.AppDatabase
import com.lyrismet.dndcodex.domain.model.SessionNote
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SessionNoteRepositoryImpl(
    private val database: AppDatabase,
) : SessionNoteRepository {
    private val queries = database.sessionNoteQueries
    private val mentionQueries = database.sessionNoteMentionQueries

    override fun observeAll(): Flow<List<SessionNote>> = queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): SessionNote? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    override suspend fun upsert(sessionNote: SessionNote): Long =
        withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val id =
                    if (sessionNote.id == 0L) {
                        queries.insert(sessionNote.title, sessionNote.content, sessionNote.sessionDate, nowEpochMillis())
                        queries.lastInsertRowId().executeAsOne()
                    } else {
                        queries.update(sessionNote.title, sessionNote.content, sessionNote.sessionDate, sessionNote.id)
                        sessionNote.id
                    }
                replaceMentions(id, sessionNote)
                id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    private fun replaceMentions(
        sessionNoteId: Long,
        sessionNote: SessionNote,
    ) {
        mentionQueries.deleteNpcMentions(sessionNoteId)
        mentionQueries.deleteQuestMentions(sessionNoteId)
        mentionQueries.deleteLocationMentions(sessionNoteId)
        sessionNote.mentionedNpcIds.forEach { mentionQueries.insertNpcMention(sessionNoteId, it) }
        sessionNote.mentionedQuestIds.forEach { mentionQueries.insertQuestMention(sessionNoteId, it) }
        sessionNote.mentionedLocationIds.forEach { mentionQueries.insertLocationMention(sessionNoteId, it) }
    }

    private fun toDomain(
        id: Long,
        title: String,
        content: String,
        sessionDate: LocalDateTime,
        createdAt: Long,
    ) = SessionNote(
        id = id,
        title = title,
        content = content,
        sessionDate = sessionDate,
        mentionedNpcIds = mentionQueries.selectMentionedNpcIds(id).executeAsList(),
        mentionedQuestIds = mentionQueries.selectMentionedQuestIds(id).executeAsList(),
        mentionedLocationIds = mentionQueries.selectMentionedLocationIds(id).executeAsList(),
    )

    @OptIn(ExperimentalTime::class)
    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
