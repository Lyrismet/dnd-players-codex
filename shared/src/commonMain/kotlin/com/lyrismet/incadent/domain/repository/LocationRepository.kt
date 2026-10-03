package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeAll(): Flow<List<Location>>

    suspend fun getById(id: Long): Location?

    suspend fun upsert(location: Location): Long

    suspend fun delete(id: Long)
}
