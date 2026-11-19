package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember

interface PartyRepository : EntityRepository<PartyMember> {
    /** a character who fell in [combatId] leaves the party, a revived one comes back, saving a form never changes it */
    suspend fun setFallen(
        id: Long,
        fallen: Boolean,
        combatId: Long? = null,
    )

    /** the shields of a character, a form save never touches them */
    suspend fun setVulnerabilities(
        id: Long,
        vulnerabilities: Map<DamageType, DamageState>,
    )

    /** one tap on a shield, read and written together so two taps never lose each other */
    suspend fun setVulnerability(
        id: Long,
        type: DamageType,
        state: DamageState?,
    )
}
