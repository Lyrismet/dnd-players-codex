package com.lyrismet.incadent.feature.session.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.model.DowntimeItem
import com.lyrismet.incadent.feature.session.domain.repository.DowntimeRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DowntimeRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : DowntimeRepository {
    private val queries = database.downtimeQueries

    override fun observeAll(): Flow<List<Downtime>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)
        }

    override suspend fun getById(id: Long): Downtime? =
        withContext(DatabaseDispatcher) { queries.selectById(id, ::toDomain).executeAsOneOrNull() }

    override suspend fun upsert(downtime: Downtime): Long =
        withContext(DatabaseDispatcher) {
            val items = encodeItems(downtime.items)
            val applied = if (downtime.isApplied) 1L else 0L
            val member = downtime.appliedTo ?: 0L
            if (downtime.id != 0L && queries.selectById(downtime.id).executeAsOneOrNull() != null) {
                queries.update(
                    downtime.shortText,
                    downtime.longText,
                    downtime.gainedText,
                    downtime.goldDelta.toLong(),
                    items,
                    applied,
                    member,
                    downtime.id,
                )
                downtime.id
            } else {
                database.transactionWithResult {
                    queries.insert(
                        current.requireId(),
                        downtime.afterSessionId,
                        downtime.shortText,
                        downtime.longText,
                        downtime.gainedText,
                        downtime.goldDelta.toLong(),
                        items,
                        applied,
                        member,
                        downtime.originId.orNewOriginId(),
                    )
                    queries.lastInsertRowId().executeAsOne()
                }
            }
        }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }

    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        afterSessionId: Long,
        shortText: String,
        longText: String,
        gainedText: String,
        goldDelta: Long,
        items: String,
        applied: Long,
        memberId: Long,
        originId: String,
    ) = Downtime(
        id = id,
        afterSessionId = afterSessionId,
        shortText = shortText,
        longText = longText,
        gainedText = gainedText,
        goldDelta = goldDelta.toInt(),
        items = decodeItems(items),
        appliedTo = if (applied != 0L) memberId else null,
        originId = originId,
    )
}

@Serializable
private class StoredItem(
    val name: String,
    val quantity: Int = 1,
    val itemId: Long = 0,
)

private fun encodeItems(items: List<DowntimeItem>): String =
    if (items.isEmpty()) "" else Json.encodeToString(items.map { StoredItem(it.name, it.quantity, it.itemId) })

private fun decodeItems(text: String): List<DowntimeItem> =
    if (text.isEmpty()) {
        emptyList()
    } else {
        runCatching { Json.decodeFromString<List<StoredItem>>(text) }
            .getOrDefault(emptyList())
            .map { DowntimeItem(it.name, it.quantity, it.itemId) }
    }
