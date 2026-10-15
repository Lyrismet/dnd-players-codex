package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.data.campaign.CurrentCampaign
import com.lyrismet.incadent.data.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Photo
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
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : NpcRepository {
    private val queries = database.npcQueries
    private val locationQueries = database.npcLocationQueries

    override fun observeAll(): Flow<List<Npc>> =
        current.observe { campaignId ->
            queries.selectAllWithLocations(campaignId, ::toDomainWithLocations).asFlow().mapToList(Dispatchers.Default)
        }

    override suspend fun getById(id: Long): Npc? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()?.let { npc ->
                npc.copy(locationIds = locationQueries.selectByNpc(id).executeAsList())
            }
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Npc): Long =
        withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val stored =
                    if (entity.id ==
                        0L
                    ) {
                        null
                    } else {
                        queries.selectById(entity.id, ::toDomainFull).executeAsOneOrNull()
                    }
                val id =
                    if (stored != null) {
                        queries.update(
                            entity.name,
                            entity.status,
                            entity.description,
                            entity.race,
                            entity.faction,
                            entity.lifeState,
                            entity.id,
                        )
                        writePortrait(entity.id, entity.portrait, stored.portrait)
                        entity.id
                    } else {
                        insert(entity)
                    }
                writeLocations(id, entity.locationIds)
                id
            }
        }

    private fun insert(npc: Npc): Long {
        if (npc.id != 0L) {
            queries.insertWithId(
                npc.id,
                npc.name,
                npc.status,
                npc.description,
                npc.race,
                npc.faction,
                npc.lifeState,
                npc.portrait?.full,
                npc.portrait?.thumb,
                current.requireId(),
            )
            return npc.id
        }
        queries.insert(
            npc.name,
            npc.status,
            npc.description,
            npc.race,
            npc.faction,
            npc.lifeState,
            npc.portrait?.full,
            npc.portrait?.thumb,
            current.requireId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // null removes the picture, see needsWrite for what leaves it alone
    private fun writePortrait(
        npcId: Long,
        portrait: Photo?,
        stored: Photo?,
    ) {
        when {
            portrait == null -> if (stored != null) queries.setPortrait(null, null, npcId)
            portrait.needsWrite(stored) -> queries.setPortrait(portrait.full, portrait.thumb, npcId)
        }
    }

    private fun writeLocations(
        npcId: Long,
        locationIds: List<Long>,
    ) {
        locationQueries.deleteByNpc(npcId)
        locationIds.distinct().forEach { locationQueries.insert(npcId, it) }
    }

    // location_ids is the comma separated group_concat of the npc's place ids, empty when it has none
    @Suppress("LongParameterList")
    private fun toDomainWithLocations(
        id: Long,
        name: String,
        status: NpcStatus,
        description: String,
        race: String,
        faction: String,
        lifeState: NpcLifeState,
        portraitThumb: ByteArray?,
        locationIds: String,
    ) = toDomain(id, name, status, description, race, faction, lifeState, portraitThumb)
        .copy(locationIds = locationIds.split(',').mapNotNull { it.toLongOrNull() })

    // one param per npc table column - dictated by SQLDelight's generated query mapper shape, not real complexity
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        status: NpcStatus,
        description: String,
        race: String,
        faction: String,
        lifeState: NpcLifeState,
        portraitThumb: ByteArray?,
    ) = Npc(id, name, status, lifeState, description, emptyList(), race, faction, portraitThumb?.let { Photo(it) })

    // the by-id row also carries the full jpeg
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        status: NpcStatus,
        description: String,
        race: String,
        faction: String,
        lifeState: NpcLifeState,
        portraitThumb: ByteArray?,
        portrait: ByteArray?,
    ) = toDomain(id, name, status, description, race, faction, lifeState, portraitThumb)
        .copy(portrait = portraitThumb?.let { Photo(it, portrait) })
}
