package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.grouping.decodeVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.grouping.encodeVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.CustomCreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.SpeciesRepository
import com.lyrismet.incadent.persistence.creatureTypeIdIn
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
    private val typeQueries = database.creatureTypeQueries

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
                        liveTypeId(entity),
                        entity.vulnerabilities.encodeVulnerabilities(),
                        entity.notes,
                        entity.hitPoints?.toLong(),
                        entity.id,
                    )
                    writePhotoIfChanged(
                        entity.photo,
                        stored.photo,
                    ) { full, thumb -> queries.setPhoto(full, thumb, entity.id) }
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
                species.originId.orNewOriginId(),
                liveTypeId(species),
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
            species.originId.orNewOriginId(),
            liveTypeId(species),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) {
            queries.deleteById(id)
        }
    }

    override fun observeCustomTypes(): Flow<List<CustomCreatureType>> =
        current.observe { campaignId ->
            typeQueries.selectAll(campaignId, ::CustomCreatureType).asFlow().mapToList(DatabaseDispatcher)
        }

    override suspend fun createCustomType(name: String): CustomCreatureType =
        withContext(DatabaseDispatcher) {
            database.transactionWithResult { customTypeNamed(name.toTypeName()) }
        }

    override suspend fun renameCustomType(
        id: Long,
        name: String,
    ) {
        withContext(DatabaseDispatcher) {
            val newName = name.toTypeName()
            val taken =
                typeQueries
                    .selectByName(
                        current.requireId(),
                        newName,
                        ::CustomCreatureType,
                    ).executeAsOneOrNull()
            if (newName.isNotEmpty() && (taken == null || taken.id == id)) typeQueries.rename(newName, id)
        }
    }

    override suspend fun deleteCustomType(id: Long) {
        withContext(DatabaseDispatcher) { typeQueries.deleteById(id) }
    }

    // the type of this name in the open campaign, made when it is new
    private fun customTypeNamed(name: String): CustomCreatureType {
        val id = checkNotNull(database.creatureTypeIdIn(current.requireId(), name)) { "a type needs a name" }
        return CustomCreatureType(id, name)
    }

    // a type deleted since the species was read would break the foreign key, so the link is dropped
    private fun liveTypeId(species: Species): Long? =
        species.customType?.id?.takeIf { typeQueries.selectById(it, ::CustomCreatureType).executeAsOneOrNull() != null }

    private fun String.toTypeName() = trim().lowercase()

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
        originId: String,
        customTypeId: Long?,
        customTypeName: String?,
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
        originId,
        if (customTypeId != null && customTypeName != null) CustomCreatureType(customTypeId, customTypeName) else null,
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
        originId: String,
        customTypeId: Long?,
        customTypeName: String?,
    ) = toDomain(
        id,
        name,
        creatureType,
        vulnerabilities,
        notes,
        hitPoints,
        photoThumb,
        originId,
        customTypeId,
        customTypeName,
    ).copy(photo = photoThumb?.let { Photo(it, photo) })
}
