package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import kotlinx.coroutines.flow.Flow

interface CharacterSpellRepository {
    fun observe(memberId: Long): Flow<List<CharacterSpell>>

    /** inserts when [spell] has no id, updates otherwise, and returns the id */
    suspend fun upsert(
        memberId: Long,
        spell: CharacterSpell,
    ): Long

    /** moves the free casts left by [delta], held between zero and the max */
    suspend fun changeFree(
        id: Long,
        delta: Int,
    )

    suspend fun delete(id: Long)
}
