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
    private val mentionRenamer: MentionRenamer,
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
                queries.insert(location.name, location.description, location.parentId)
                queries.lastInsertRowId().executeAsOne()
            } else {
                val oldName = queries.selectById(location.id, ::toDomain).executeAsOneOrNull()?.name
                val update: () -> Unit = {
                    queries.update(location.name, location.description, location.parentId, location.id)
                }
                if (oldName != null) {
                    mentionRenamer.locationRenamed(location.id, oldName, location.name, update)
                } else {
                    update()
                }
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
        description: String,
        parentId: Long?,
    ) = Location(id, name, description, parentId)
}
