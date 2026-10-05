package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import kotlinx.coroutines.flow.Flow

interface NpcRepository {
    fun observeAll(): Flow<List<Npc>>

    fun observeByLocation(locationId: Long): Flow<List<Npc>>

    suspend fun getById(id: Long): Npc?

    suspend fun upsert(npc: Npc): Long

    suspend fun delete(id: Long)
}

suspend fun NpcRepository.updateStatus(
    npcId: Long,
    status: NpcStatus,
) {
    getById(npcId)?.let { npc -> upsert(npc.copy(status = status)) }
}

suspend fun NpcRepository.updateLifeState(
    npcId: Long,
    lifeState: NpcLifeState,
) {
    getById(npcId)?.let { npc -> upsert(npc.copy(lifeState = lifeState)) }
}
