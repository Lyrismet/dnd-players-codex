package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.repository.LocationRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PlaceCycleException
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
class LocationRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : LocationRepository {
    private val queries = database.locationQueries

    override fun observeAll(): Flow<List<Location>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(Dispatchers.Default)
        }

    override suspend fun getById(id: Long): Location? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Location): Long =
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
                if (stored != null) {
                    if (entity.parentId != stored.parentId) checkNoCycle(entity.id, entity.parentId)
                    queries.update(
                        entity.name,
                        entity.description,
                        entity.parentId,
                        entity.id,
                    )
                    writeCover(entity.id, entity.cover, stored.cover)
                    entity.id
                } else {
                    insert(entity)
                }
            }
        }

    // the same rule the pickers apply, held here so no path can write a loop
    private fun checkNoCycle(
        id: Long,
        parentId: Long?,
    ) {
        if (parentId == null) return
        val tree = PlaceTree(queries.selectAll(current.requireId(), ::toDomain).executeAsList())
        if (!tree.canPutInside(id, parentId)) throw PlaceCycleException()
    }

    // a single column write, the photo is never read
    override suspend fun setParent(
        id: Long,
        parentId: Long?,
    ) {
        withContext(Dispatchers.Default) {
            database.transaction {
                checkNoCycle(id, parentId)
                queries.setParent(parentId, id)
            }
        }
    }

    private fun insert(location: Location): Long {
        if (location.id != 0L) {
            queries.insertWithId(
                location.id,
                location.name,
                location.description,
                location.parentId,
                location.cover?.full,
                location.cover?.thumb,
                current.requireId(),
            )
            return location.id
        }
        queries.insert(
            location.name,
            location.description,
            location.parentId,
            location.cover?.full,
            location.cover?.thumb,
            current.requireId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    // null removes the picture, see needsWrite for what leaves it alone
    private fun writeCover(
        locationId: Long,
        cover: Photo?,
        stored: Photo?,
    ) {
        when {
            cover == null -> if (stored != null) queries.setCover(null, null, locationId)
            cover.needsWrite(stored) -> queries.setCover(cover.full, cover.thumb, locationId)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // one param per location column - dictated by SQLDelight's generated query mapper shape
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        description: String,
        parentId: Long?,
        coverThumb: ByteArray?,
    ) = Location(id, name, description, parentId, coverThumb?.let { Photo(it) })

    // the by-id row also carries the full jpeg - one param per column, dictated by the generated mapper
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        description: String,
        parentId: Long?,
        coverThumb: ByteArray?,
        cover: ByteArray?,
    ) = toDomain(
        id,
        name,
        description,
        parentId,
        coverThumb,
    ).copy(cover = coverThumb?.let { Photo(it, cover) })
}
