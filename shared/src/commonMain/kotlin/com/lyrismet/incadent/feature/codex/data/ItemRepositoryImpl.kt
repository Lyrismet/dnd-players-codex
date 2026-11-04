package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_PRICE_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.repository.ItemRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ItemRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : ItemRepository {
    private val queries = database.itemQueries

    override fun observeAll(): Flow<List<Item>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)
        }

    override suspend fun getById(id: Long): Item? =
        withContext(DatabaseDispatcher) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    override suspend fun upsert(entity: Item): Long =
        withContext(DatabaseDispatcher) {
            database.transactionWithResult {
                val item = entity.clamped()
                val stored = queries.selectById(item.id, ::toDomainFull).executeAsOneOrNull()
                if (stored != null) {
                    queries.update(
                        item.name,
                        item.quantity.toLong(),
                        item.price?.toLong(),
                        item.coin,
                        item.owner.kind(),
                        item.owner.id(),
                        item.notes,
                        item.id,
                    )
                    writePhotoIfChanged(item.photo, stored.photo) { full, thumb ->
                        queries.setPhoto(full, thumb, item.id)
                    }
                    item.id
                } else {
                    insert(item)
                }
            }
        }

    private fun insert(item: Item): Long {
        if (item.id != 0L) {
            queries.insertWithId(
                item.id,
                item.name,
                item.quantity.toLong(),
                item.price?.toLong(),
                item.coin,
                item.owner.kind(),
                item.owner.id(),
                item.notes,
                item.photo?.full,
                item.photo?.thumb,
                current.requireId(),
                item.originId.orNewOriginId(),
            )
            return item.id
        }
        queries.insert(
            item.name,
            item.quantity.toLong(),
            item.price?.toLong(),
            item.coin,
            item.owner.kind(),
            item.owner.id(),
            item.notes,
            item.photo?.full,
            item.photo?.thumb,
            current.requireId(),
            item.originId.orNewOriginId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }

    override suspend fun setOwner(
        ids: Collection<Long>,
        owner: ItemOwner,
    ) {
        withContext(DatabaseDispatcher) {
            database.transaction {
                ids.forEach { id ->
                    val stored = queries.selectById(id, ::toDomainFull).executeAsOneOrNull() ?: return@forEach
                    queries.update(
                        stored.name,
                        stored.quantity.toLong(),
                        stored.price?.toLong(),
                        stored.coin,
                        owner.kind(),
                        owner.id(),
                        stored.notes,
                        id,
                    )
                }
            }
        }
    }

    private fun Item.clamped() =
        copy(quantity = quantity.coerceIn(ITEM_QUANTITY_RANGE), price = price?.coerceIn(ITEM_PRICE_RANGE))

    private fun ItemOwner.kind(): EntityKind? = (this as? ItemOwner.Of)?.kind

    private fun ItemOwner.id(): Long = (this as? ItemOwner.Of)?.id ?: 0L

    // one param per item column - dictated by SQLDelight's generated query mapper shape
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        quantity: Long,
        price: Long?,
        coin: Coin,
        ownerKind: EntityKind?,
        ownerId: Long,
        notes: String,
        photoThumb: ByteArray?,
        originId: String,
    ) = Item(
        id,
        name,
        quantity.toInt(),
        price?.toInt(),
        coin,
        if (ownerKind != null) ItemOwner.Of(ownerKind, ownerId) else ItemOwner.Shared,
        notes,
        photoThumb?.let { Photo(it) },
        originId,
    )

    // the by-id row also carries the full jpeg
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        quantity: Long,
        price: Long?,
        coin: Coin,
        ownerKind: EntityKind?,
        ownerId: Long,
        notes: String,
        photoThumb: ByteArray?,
        photo: ByteArray?,
        originId: String,
    ) = toDomain(id, name, quantity, price, coin, ownerKind, ownerId, notes, photoThumb, originId)
        .copy(photo = photoThumb?.let { Photo(it, photo) })
}
