package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.repository.LocationRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PlaceCycleException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)
        }

    override suspend fun getById(id: Long): Location? =
        withContext(DatabaseDispatcher) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Location): Long =
        withContext(DatabaseDispatcher) {
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
                    writePhotoIfChanged(
                        entity.cover,
                        stored.cover,
                    ) { full, thumb -> queries.setCover(full, thumb, entity.id) }
                    entity.id
                } else {
                    insert(entity)
                }
            }
        }

    // the same rule the pickers apply, held here so no path can write a loop - walks up from the new parent
    private fun checkNoCycle(
        id: Long,
        parentId: Long?,
    ) {
        val seen = HashSet<Long>()
        var next = parentId
        while (next != null && seen.add(next)) {
            if (next == id) throw PlaceCycleException()
            next = queries.selectParentId(next).executeAsOneOrNull()?.parent_id
        }
    }

    // a single column write, the photo is never read
    override suspend fun setParent(
        id: Long,
        parentId: Long?,
    ) = setParents(mapOf(id to parentId))

    // one transaction, so a move the rule refuses rolls the earlier ones back
    override suspend fun setParents(parents: Map<Long, Long?>) {
        withContext(DatabaseDispatcher) {
            database.transaction {
                parents.forEach { (id, parentId) ->
                    checkNoCycle(id, parentId)
                    queries.setParent(parentId, id)
                }
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
                location.originId.orNewOriginId(),
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
            location.originId.orNewOriginId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) {
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
        originId: String,
    ) = Location(id, name, description, parentId, coverThumb?.let { Photo(it) }, originId)

    // the by-id row also carries the full jpeg - one param per column, dictated by the generated mapper
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        description: String,
        parentId: Long?,
        coverThumb: ByteArray?,
        cover: ByteArray?,
        originId: String,
    ) = toDomain(
        id,
        name,
        description,
        parentId,
        coverThumb,
        originId,
    ).copy(cover = coverThumb?.let { Photo(it, cover) })
}
