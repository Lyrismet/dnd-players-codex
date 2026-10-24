package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember

interface PartyRepository : EntityRepository<PartyMember> {
    /** a character who fell in [combatId] leaves the party, a revived one comes back, saving a form never changes it */
    suspend fun setFallen(
        id: Long,
        fallen: Boolean,
        combatId: Long? = null,
    )
}
