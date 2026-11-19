package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import kotlinx.coroutines.flow.Flow

interface CharacterFeatRepository {
    fun observe(memberId: Long): Flow<List<CharacterFeat>>

    suspend fun getById(id: Long): CharacterFeat?

    /** inserts when [feat] has no id or its id is gone, updates otherwise, and returns the id */
    suspend fun upsert(
        memberId: Long,
        feat: CharacterFeat,
    ): Long

    suspend fun delete(id: Long)
}
