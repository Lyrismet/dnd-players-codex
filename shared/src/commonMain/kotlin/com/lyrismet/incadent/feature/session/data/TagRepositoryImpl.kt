package com.lyrismet.incadent.feature.session.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.session.domain.model.BUILT_IN_SESSION_TAGS
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TagRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : TagRepository {
    private val tagQueries = database.tagQueries
    private val sessionTagQueries = database.sessionTagQueries

    override fun observeCatalog(): Flow<List<String>> =
        current.observe { campaignId ->
            tagQueries
                .selectAll(campaignId)
                .asFlow()
                .mapToList(DatabaseDispatcher)
                .map { rows ->
                    val custom = rows.map { it.name }.filterNot { name -> name.isBuiltIn() }
                    BUILT_IN_SESSION_TAGS + custom
                }
        }

    override fun observeTagsFor(sessionNoteId: Long): Flow<List<String>> =
        sessionTagQueries.selectTagNamesForSession(sessionNoteId).asFlow().mapToList(DatabaseDispatcher)

    override fun observeAllSessionTags(): Flow<Map<Long, List<String>>> =
        current.observe { campaignId ->
            sessionTagQueries
                .selectAllSessionTagNames(campaignId)
                .asFlow()
                .mapToList(DatabaseDispatcher)
                .map { rows -> rows.groupBy({ it.session_note_id }, { it.name }) }
        }

    override suspend fun addTag(
        sessionNoteId: Long,
        name: String,
    ) {
        withContext(DatabaseDispatcher) {
            val tagId = ensureTagId(name)
            sessionTagQueries.insert(sessionNoteId, tagId)
        }
    }

    override suspend fun removeTag(
        sessionNoteId: Long,
        name: String,
    ) {
        withContext(DatabaseDispatcher) {
            val tagId = findTagId(name) ?: return@withContext
            sessionTagQueries.delete(sessionNoteId, tagId)
        }
    }

    // sqlite's own NOCASE collation only folds ascii, so an existing name is matched in kotlin instead
    private fun findTagId(name: String): Long? =
        tagQueries
            .selectAll(current.requireId())
            .executeAsList()
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.id

    // insert is OR IGNORE so a race is swallowed, id is re-read by name since lastInsertRowId would be stale then
    private fun ensureTagId(name: String): Long {
        findTagId(name)?.let { return it }
        tagQueries.insert(name, current.requireId())
        return findTagId(name) ?: error("tag '$name' missing immediately after insert")
    }

    private fun String.isBuiltIn(): Boolean = BUILT_IN_SESSION_TAGS.any { it.equals(this, ignoreCase = true) }
}
