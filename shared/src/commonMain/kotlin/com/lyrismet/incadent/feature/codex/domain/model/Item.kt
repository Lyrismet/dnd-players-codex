package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo

/** a thing somebody carries - a stack of [quantity] pieces, each worth [price] when it has a price */
data class Item(
    val id: Long,
    val name: String,
    val quantity: Int = 1,
    // per piece, null while nobody priced it
    val price: Int? = null,
    val coin: Coin = Coin.GOLD,
    val owner: ItemOwner = ItemOwner.Shared,
    val notes: String = "",
    val photo: Photo? = null,
    val originId: String = "",
) {
    /** one piece less, null when this was the last piece and the item goes away */
    fun withOneLess(): Item? = if (quantity <= ITEM_QUANTITY_RANGE.first) null else copy(quantity = quantity - 1)

    fun withOneMore(): Item = copy(quantity = (quantity + 1).coerceAtMost(ITEM_QUANTITY_RANGE.last))

    fun withQuantity(value: Int): Item = copy(quantity = value.coerceIn(ITEM_QUANTITY_RANGE))
}

/** who carries an item, a codex record by kind and id or nobody in particular */
sealed interface ItemOwner {
    data object Shared : ItemOwner

    data class Of(
        val kind: EntityKind,
        val id: Long,
    ) : ItemOwner
}

/** the records that can carry things */
val ITEM_OWNER_KINDS = listOf(EntityKind.PARTY, EntityKind.NPC, EntityKind.LOCATION)

val ITEM_QUANTITY_RANGE = 1..9999
val ITEM_PRICE_RANGE = 0..9_999_999

enum class Coin { PLATINUM, GOLD, ELECTRUM, SILVER, COPPER }
