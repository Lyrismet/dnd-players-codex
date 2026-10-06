package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    fun observeAll(): Flow<List<Quest>>

    fun observeByStatus(status: QuestStatus): Flow<List<Quest>>

    suspend fun getById(id: Long): Quest?

    suspend fun upsert(quest: Quest): Long

    suspend fun delete(id: Long)
}
