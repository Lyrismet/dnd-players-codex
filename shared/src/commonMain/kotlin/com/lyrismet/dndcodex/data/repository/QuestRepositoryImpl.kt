package com.lyrismet.dndcodex.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.dndcodex.db.AppDatabase
import com.lyrismet.dndcodex.domain.model.Quest
import com.lyrismet.dndcodex.domain.model.QuestStatus
import com.lyrismet.dndcodex.domain.repository.QuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
                queries.insert(quest.title, quest.status, quest.reward, quest.givenByNpcId, quest.locationId)
                queries.lastInsertRowId().executeAsOne()
            } else {
                queries.update(quest.title, quest.status, quest.reward, quest.givenByNpcId, quest.locationId, quest.id)
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
    ) = Quest(id, title, status, reward, givenByNpcId, locationId)
}
