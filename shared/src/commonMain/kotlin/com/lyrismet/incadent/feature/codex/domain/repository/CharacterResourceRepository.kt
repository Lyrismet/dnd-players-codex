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

    suspend fun delete(id: Long)
}
