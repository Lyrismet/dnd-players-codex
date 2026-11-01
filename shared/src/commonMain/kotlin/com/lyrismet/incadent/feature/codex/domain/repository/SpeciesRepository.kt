package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CustomCreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import kotlinx.coroutines.flow.Flow

interface SpeciesRepository : EntityRepository<Species> {
    fun observeCustomTypes(): Flow<List<CustomCreatureType>>

    /** makes a type of this name, or returns the one that already has it */
    suspend fun createCustomType(name: String): CustomCreatureType

    suspend fun renameCustomType(
        id: Long,
        name: String,
    )

    /** the species that had it keep everything else and show no type */
    suspend fun deleteCustomType(id: Long)
}
