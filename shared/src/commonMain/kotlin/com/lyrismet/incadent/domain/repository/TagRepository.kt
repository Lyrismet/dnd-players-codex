package com.lyrismet.incadent.domain.repository

import kotlinx.coroutines.flow.Flow

/** the session-tag catalog (built-in tags plus every custom one ever added) and its sessions assignments */
interface TagRepository {
    /** every tag name ever available for assignment - the 7 built-ins plus all custom tags added so far */
    fun observeCatalog(): Flow<List<String>>

    fun observeTagsFor(sessionNoteId: Long): Flow<List<String>>

    /** every session's assigned tag names, keyed by session note id - for the archive list's tag badges */
    fun observeAllSessionTags(): Flow<Map<Long, List<String>>>

    /** adds [name] to the catalog if it isn't there yet, then assigns it to the session, in one call */
    suspend fun addTag(
        sessionNoteId: Long,
        name: String,
    )

    /** unassigns [name] from the session - the catalog keeps the tag for reuse by other sessions */
    suspend fun removeTag(
        sessionNoteId: Long,
        name: String,
    )
}
