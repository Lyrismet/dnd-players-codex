package com.lyrismet.dndcodex.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.dndcodex.db.AppDatabase
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NpcRepositoryImpl(
    database: AppDatabase,
) : NpcRepository {
    private val queries = database.npcQueries

    override fun observeAll(): Flow<List<Npc>> = queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override fun observeByLocation(locationId: Long): Flow<List<Npc>> =
        queries.selectByLocation(locationId, ::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): Npc? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    override suspend fun upsert(npc: Npc): Long =
        withContext(Dispatchers.Default) {
            if (npc.id == 0L) {
                queries.insert(npc.name, npc.status, npc.description, npc.locationId)
                queries.lastInsertRowId().executeAsOne()
            } else {
                queries.update(npc.name, npc.status, npc.description, npc.locationId, npc.id)
                npc.id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    private fun toDomain(
        id: Long,
        name: String,
        status: NpcStatus,
        description: String,
        locationId: Long?,
    ) = Npc(id, name, status, description, locationId)
}
