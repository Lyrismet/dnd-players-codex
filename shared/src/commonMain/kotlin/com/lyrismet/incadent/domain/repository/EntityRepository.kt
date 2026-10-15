package com.lyrismet.incadent.domain.repository

import kotlinx.coroutines.flow.Flow

/** what every codex record store offers - the open campaign's records, one by id, and writes */
interface EntityRepository<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun getById(id: Long): T?

    /** inserts when [entity] has no id or its id is gone, updates otherwise, and returns the id */
    suspend fun upsert(entity: T): Long

    suspend fun delete(id: Long)
}
