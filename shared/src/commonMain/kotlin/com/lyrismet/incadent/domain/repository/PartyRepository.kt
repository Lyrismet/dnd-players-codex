package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.PartyMember
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeAll(): Flow<List<PartyMember>>

    suspend fun getById(id: Long): PartyMember?

    suspend fun upsert(member: PartyMember): Long

    suspend fun delete(id: Long)
}
