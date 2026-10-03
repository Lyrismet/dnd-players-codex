package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.CombatScratchpad
import kotlinx.coroutines.flow.Flow

interface CombatScratchpadRepository {
    fun observeAll(): Flow<List<CombatScratchpad>>

    suspend fun getById(id: Long): CombatScratchpad?

    suspend fun upsert(
        scratchpad: CombatScratchpad,
        sessionNoteId: Long?,
    ): Long

    suspend fun updateBossHp(
        id: Long,
        hpCurrent: Int,
    )

    suspend fun updateCombatantHp(
        combatantId: Long,
        hpCurrent: Int,
    )

    suspend fun delete(id: Long)
}
