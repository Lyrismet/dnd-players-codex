package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.repository.NpcRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NpcRepositoryImpl(
    database: AppDatabase,
    private val mentionRenamer: MentionRenamer,
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
                queries.insert(
                    npc.name,
                    npc.status,
                    npc.description,
                    npc.locationId,
                    npc.race,
                    npc.faction,
                    npc.lifeState,
                    npc.portraitBase64,
                )
                queries.lastInsertRowId().executeAsOne()
            } else {
                val oldName = queries.selectById(npc.id, ::toDomain).executeAsOneOrNull()?.name
                queries.update(
                    npc.name,
                    npc.status,
                    npc.description,
                    npc.locationId,
                    npc.race,
                    npc.faction,
                    npc.lifeState,
                    npc.portraitBase64,
                    npc.id,
                )
                if (oldName != null) mentionRenamer.npcRenamed(npc.id, oldName, npc.name)
                npc.id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // one param per npc table column - dictated by SQLDelight's generated query mapper shape, not real complexity
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        status: NpcStatus,
        description: String,
        locationId: Long?,
        race: String,
        faction: String,
        lifeState: NpcLifeState,
        portraitBase64: String?,
    ) = Npc(id, name, status, lifeState, description, locationId, race, faction, portraitBase64)
}
