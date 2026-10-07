package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.tags.BUILT_IN_SESSION_TAGS
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TagRepositoryImpl(
    database: AppDatabase,
) : TagRepository {
    private val tagQueries = database.tagQueries
    private val sessionTagQueries = database.sessionTagQueries

    override fun observeCatalog(): Flow<List<String>> =
        tagQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                val custom = rows.map { it.name }.filterNot { name -> name.isBuiltIn() }
                BUILT_IN_SESSION_TAGS + custom
            }

    override fun observeTagsFor(sessionNoteId: Long): Flow<List<String>> =
        sessionTagQueries.selectTagNamesForSession(sessionNoteId).asFlow().mapToList(Dispatchers.Default)

    override fun observeAllSessionTags(): Flow<Map<Long, List<String>>> =
        sessionTagQueries
            .selectAllSessionTagNames()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.groupBy({ it.session_note_id }, { it.name }) }

    override suspend fun addTag(
        sessionNoteId: Long,
        name: String,
    ) {
        withContext(Dispatchers.Default) {
            val tagId = ensureTagId(name)
            sessionTagQueries.insert(sessionNoteId, tagId)
        }
    }

    override suspend fun removeTag(
        sessionNoteId: Long,
        name: String,
    ) {
        withContext(Dispatchers.Default) {
            val tagId = findTagId(name) ?: return@withContext
            sessionTagQueries.delete(sessionNoteId, tagId)
        }
    }

    // sqlite's own NOCASE collation only folds ascii, so an existing name is matched in kotlin instead
    private fun findTagId(name: String): Long? =
        tagQueries
            .selectAll()
            .executeAsList()
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.id

    private fun ensureTagId(name: String): Long =
        findTagId(name) ?: run {
            tagQueries.insert(name)
            tagQueries.lastInsertRowId().executeAsOne()
        }

    private fun String.isBuiltIn(): Boolean = BUILT_IN_SESSION_TAGS.any { it.equals(this, ignoreCase = true) }
}
