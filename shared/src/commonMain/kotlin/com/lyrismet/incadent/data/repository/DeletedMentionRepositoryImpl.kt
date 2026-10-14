package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.data.campaign.CurrentCampaign
import com.lyrismet.incadent.data.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.repository.DeletedMentionRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DeletedMentionRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : DeletedMentionRepository {
    private val queries = database.deletedMentionQueries

    override fun observeAll(): Flow<List<DeletedMention>> =
        current.observe { campaignId ->
            queries
                .selectAll(campaignId)
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { rows ->
                    // a type this build does not know is dropped instead of crashing the whole feed
                    rows.mapNotNull { row ->
                        DeletedMentionType.entries
                            .firstOrNull { it.name == row.type }
                            ?.let { DeletedMention(row.name, it) }
                    }
                }
        }

    override suspend fun remember(mention: DeletedMention) {
        withContext(Dispatchers.Default) { queries.upsert(mention.name, mention.type.name, current.requireId()) }
    }

    override suspend fun forget(mention: DeletedMention) {
        withContext(Dispatchers.Default) { queries.delete(mention.name, mention.type.name, current.requireId()) }
    }
}
