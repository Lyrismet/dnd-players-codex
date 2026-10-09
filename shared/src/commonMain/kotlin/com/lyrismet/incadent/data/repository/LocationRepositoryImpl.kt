package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.repository.LocationRepository
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
    database: AppDatabase,
) : LocationRepository {
    private val queries = database.locationQueries

    override fun observeAll(): Flow<List<Location>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): Location? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    override suspend fun upsert(location: Location): Long =
        withContext(Dispatchers.Default) {
            if (location.id == 0L) {
                queries.insert(location.name, location.type, location.description, location.region)
                queries.lastInsertRowId().executeAsOne()
            } else {
                queries.update(location.name, location.type, location.description, location.region, location.id)
                location.id
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
        type: String,
        description: String,
        region: String,
    ) = Location(id, name, type, description, region)
}
