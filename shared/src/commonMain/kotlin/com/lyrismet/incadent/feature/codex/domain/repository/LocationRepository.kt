package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.Location

/** [upsert] throws [PlaceCycleException] when the parent is the place itself or sits inside it */
interface LocationRepository : EntityRepository<Location> {
    /** moves one place under [parentId], null for the top level - throws like [upsert] when that makes a loop */
    suspend fun setParent(
        id: Long,
        parentId: Long?,
    ) {
        getById(id)?.let { upsert(it.copy(parentId = parentId)) }
    }
}

/** a place was asked to go inside itself or inside what it contains */
class PlaceCycleException : IllegalArgumentException("a place can't be put inside itself or its descendants")
