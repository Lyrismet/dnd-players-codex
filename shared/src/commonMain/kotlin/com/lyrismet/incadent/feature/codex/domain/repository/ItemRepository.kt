package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner

interface ItemRepository : EntityRepository<Item> {
    /** hands every item of [ids] to [owner] in one go */
    suspend fun setOwner(
        ids: Collection<Long>,
        owner: ItemOwner,
    )

    /** moves the quantity of [id] by [delta] in one step, kept inside the allowed range */
    suspend fun stepQuantity(
        id: Long,
        delta: Int,
    )
}
