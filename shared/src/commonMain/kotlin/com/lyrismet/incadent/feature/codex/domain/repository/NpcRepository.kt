package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc

interface NpcRepository : EntityRepository<Npc> {
    /** the shields of a record read without its photo, null when the record is gone */
    suspend fun getVulnerabilities(id: Long): Map<DamageType, DamageState>?

    /** writes only the shields, false when the record is gone */
    suspend fun setVulnerabilities(
        id: Long,
        shields: Map<DamageType, DamageState>,
    ): Boolean
}
