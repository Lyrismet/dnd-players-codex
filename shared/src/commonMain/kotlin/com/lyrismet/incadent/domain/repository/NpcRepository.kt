package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.Npc
import kotlinx.coroutines.flow.Flow

interface NpcRepository {
    fun observeAll(): Flow<List<Npc>>

    fun observeByLocation(locationId: Long): Flow<List<Npc>>

    suspend fun getById(id: Long): Npc?

    suspend fun upsert(npc: Npc): Long

    suspend fun delete(id: Long)
}
