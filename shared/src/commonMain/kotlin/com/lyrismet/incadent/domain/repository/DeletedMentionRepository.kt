package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.DeletedMention
import kotlinx.coroutines.flow.Flow

/** snapshots of deleted codex records that notes still mention, one per name and kind */
interface DeletedMentionRepository {
    fun observeAll(): Flow<List<DeletedMention>>

    /** a second snapshot of the same name and kind replaces the first */
    suspend fun remember(mention: DeletedMention)

    suspend fun forget(mention: DeletedMention)
}
