package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.data.campaign.CurrentCampaign
import com.lyrismet.incadent.data.campaign.observe
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
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : QuestRepository {
    private val queries = database.questQueries

    override fun observeAll(): Flow<List<Quest>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(Dispatchers.Default)
        }

    override suspend fun getById(id: Long): Quest? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Quest): Long =
        withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val exists = entity.id != 0L && queries.selectById(entity.id, ::toDomain).executeAsOneOrNull() != null
                if (exists) {
                    queries.update(
                        entity.title,
                        entity.status,
                        entity.reward,
                        entity.givenByNpcId,
                        entity.locationId,
                        entity.description,
                        entity.id,
                    )
                    entity.id
                } else {
                    insert(entity)
                }
            }
        }

    private fun insert(quest: Quest): Long {
        if (quest.id != 0L) {
            queries.insertWithId(
                quest.id,
                quest.title,
                quest.status,
                quest.reward,
                quest.givenByNpcId,
                quest.locationId,
                quest.description,
                current.requireId(),
            )
            return quest.id
        }
        queries.insert(
            quest.title,
            quest.status,
            quest.reward,
            quest.givenByNpcId,
            quest.locationId,
            quest.description,
            current.requireId(),
        )
        return queries.lastInsertRowId().executeAsOne()
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
