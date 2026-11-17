package com.lyrismet.incadent.feature.session.domain.repository

import com.lyrismet.incadent.feature.session.domain.model.Downtime
import kotlinx.coroutines.flow.Flow

interface DowntimeRepository {
    fun observeAll(): Flow<List<Downtime>>

    suspend fun getById(id: Long): Downtime?

    /** inserts when [downtime] has no id, updates otherwise, and returns the id - a gap holds one downtime */
    suspend fun upsert(downtime: Downtime): Long

    suspend fun delete(id: Long)
}
