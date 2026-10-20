package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.grouping.InsideChanges
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.model.Location
import kotlinx.coroutines.flow.first

/** [upsert] throws [PlaceCycleException] when the parent is the place itself or sits inside it */
interface LocationRepository : EntityRepository<Location> {
    /** moves one place under [parentId], null for the top level - throws like [upsert] when that makes a loop */
    suspend fun setParent(
        id: Long,
        parentId: Long?,
    ) {
        getById(id)?.let { upsert(it.copy(parentId = parentId)) }
    }

    /** moves several places in the given order as one step - a refused move leaves all of them where they were */
    suspend fun setParents(parents: Map<Long, Long?>) {
        parents.forEach { (id, parentId) -> setParent(id, parentId) }
    }

    /** makes [parentId] hold exactly [picked], places dropped from it go to the top level - returns what moved */
    suspend fun setInside(
        parentId: Long,
        picked: Collection<Long>,
    ): InsideChanges {
        val changes = PlaceTree(observeAll().first()).insideChanges(parentId, picked)
        setParents(changes.leaving.associateWith { null } + changes.entering.associateWith { parentId })
        return changes
    }
}

/** a place was asked to go inside itself or inside what it contains */
class PlaceCycleException : IllegalArgumentException("a place can't be put inside itself or its descendants")
