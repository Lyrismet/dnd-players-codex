package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.grouping.decodeVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.grouping.encodeVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SpeciesRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : SpeciesRepository {
    private val queries = database.speciesQueries

    override fun observeAll(): Flow<List<Species>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)
        }

    override suspend fun getById(id: Long): Species? =
        withContext(DatabaseDispatcher) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Species): Long =
        withContext(DatabaseDispatcher) {
            database.transactionWithResult {
                val stored = queries.selectById(entity.id, ::toDomainFull).executeAsOneOrNull()
                if (stored != null) {
                    queries.update(
                        entity.name,
                        entity.creatureType,
                        entity.vulnerabilities.encodeVulnerabilities(),
                        entity.notes,
                        entity.hitPoints?.toLong(),
                        entity.id,
                    )
                    writePhoto(entity.id, entity.photo, stored.photo)
                    entity.id
                } else {
                    insert(entity)
                }
            }
        }

    private fun insert(species: Species): Long {
        if (species.id != 0L) {
            queries.insertWithId(
                species.id,
                species.name,
                species.creatureType,
                species.vulnerabilities.encodeVulnerabilities(),
                species.notes,
                species.hitPoints?.toLong(),
                species.photo?.full,
                species.photo?.thumb,
                current.requireId(),
            )
            return species.id
        }
        queries.insert(
            species.name,
            species.creatureType,
            species.vulnerabilities.encodeVulnerabilities(),
            species.notes,
            species.hitPoints?.toLong(),
            species.photo?.full,
            species.photo?.thumb,
            current.requireId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) {
            queries.deleteById(id)
        }
    }

    // null removes the picture, see needsWrite for what leaves it alone
    private fun writePhoto(
        speciesId: Long,
        photo: Photo?,
        stored: Photo?,
    ) {
        when {
            photo == null -> if (stored != null) queries.setPhoto(null, null, speciesId)
            photo.needsWrite(stored) -> queries.setPhoto(photo.full, photo.thumb, speciesId)
        }
    }

    // one param per species column - dictated by SQLDelight's generated query mapper shape
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        creatureType: CreatureType,
        vulnerabilities: String,
        notes: String,
        hitPoints: Long?,
        photoThumb: ByteArray?,
    ) = Species(
        id,
        name,
        creatureType,
        decodeVulnerabilities(vulnerabilities),
        notes,
        photoThumb?.let {
            Photo(it)
        },
        hitPoints?.toInt(),
    )

    // the by-id row also carries the full jpeg
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        creatureType: CreatureType,
        vulnerabilities: String,
        notes: String,
        hitPoints: Long?,
        photoThumb: ByteArray?,
        photo: ByteArray?,
    ) = toDomain(id, name, creatureType, vulnerabilities, notes, hitPoints, photoThumb)
        .copy(photo = photoThumb?.let { Photo(it, photo) })
}
