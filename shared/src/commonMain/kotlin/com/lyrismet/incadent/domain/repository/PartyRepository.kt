package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeAll(): Flow<List<PartyMember>>

    suspend fun getById(id: Long): PartyMember?

    suspend fun upsert(member: PartyMember): Long

    suspend fun delete(id: Long)
}

suspend fun PartyRepository.updatePresence(
    memberId: Long,
    presence: PartyPresence,
) {
    getById(memberId)?.let { member -> upsert(member.copy(presence = presence)) }
}
