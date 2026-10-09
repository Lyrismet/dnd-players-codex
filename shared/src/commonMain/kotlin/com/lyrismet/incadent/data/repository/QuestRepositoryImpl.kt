package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.QuestRepository
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
class QuestRepositoryImpl(
    database: AppDatabase,
) : QuestRepository {
    private val queries = database.questQueries

    override fun observeAll(): Flow<List<Quest>> = queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override fun observeByStatus(status: QuestStatus): Flow<List<Quest>> =
        queries.selectByStatus(status, ::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): Quest? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    override suspend fun upsert(quest: Quest): Long =
        withContext(Dispatchers.Default) {
            if (quest.id == 0L) {
                queries.insert(
                    quest.title,
                    quest.status,
                    quest.reward,
                    quest.givenByNpcId,
                    quest.locationId,
                    quest.description,
                )
                queries.lastInsertRowId().executeAsOne()
            } else {
                queries.update(
                    quest.title,
                    quest.status,
                    quest.reward,
                    quest.givenByNpcId,
                    quest.locationId,
                    quest.description,
                    quest.id,
                )
                quest.id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // mirrors the quest table's own column order, one param per column
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        title: String,
        status: QuestStatus,
        reward: String,
        givenByNpcId: Long?,
        locationId: Long?,
        description: String,
    ) = Quest(id, title, status, reward, givenByNpcId, locationId, description)
}
