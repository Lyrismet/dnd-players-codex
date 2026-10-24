package com.lyrismet.incadent.feature.combat.domain.repository

import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import kotlinx.coroutines.flow.Flow

interface HitObservationRepository {
    /** what past fights taught about each species, by species id */
    fun observeAll(): Flow<Map<Long, List<SpeciesHitObservation>>>

    /** the stored id is returned, a deleted combat takes its observations with it */
    suspend fun add(
        speciesId: Long,
        combatId: Long,
        observation: SpeciesHitObservation,
    ): Long

    suspend fun remove(id: Long)
}
