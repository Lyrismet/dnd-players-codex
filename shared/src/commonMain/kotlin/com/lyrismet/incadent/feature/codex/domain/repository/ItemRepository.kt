package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner

interface ItemRepository : EntityRepository<Item> {
    /** hands every item of [ids] to [owner] in one go */
    suspend fun setOwner(
        ids: Collection<Long>,
        owner: ItemOwner,
    )
}
