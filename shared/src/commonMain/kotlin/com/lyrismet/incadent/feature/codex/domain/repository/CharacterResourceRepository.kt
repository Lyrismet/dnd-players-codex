package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import kotlinx.coroutines.flow.Flow

interface CharacterResourceRepository {
    fun observe(memberId: Long): Flow<List<CharacterResource>>

    /** inserts when [resource] has no id, updates otherwise, and returns the id */
    suspend fun upsert(
        memberId: Long,
        resource: CharacterResource,
    ): Long

    /** the spent or kept count of one resource, held between zero and its max */
    suspend fun setCurrent(
        id: Long,
        value: Int,
    )

    /** a lit cell at [index] is spent and a dark one comes back, in one step so quick presses add up */
    suspend fun pressCell(
        id: Long,
        index: Int,
    )

    /** moves the count by [delta], held between zero and the max, so a cast and its undo add up */
    suspend fun change(
        id: Long,
        delta: Int,
    )

    /** takes one when the count is above zero, false when there was nothing to take */
    suspend fun spend(id: Long): Boolean

    /** renames a resource and moves its max, the count left is kept and cut to the new max */
    suspend fun edit(
        id: Long,
        name: String,
        max: Int,
    )

    suspend fun delete(id: Long)
}
