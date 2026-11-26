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

    /** whether the maximum is a guess, a typed maximum equal to the stored one is exact all the same */
    suspend fun setHpMaxApprox(
        id: Long,
        approx: Boolean,
    )

    /** the level up numbers of a character, a form save never touches them */
    suspend fun setCharacterExtras(
        id: Long,
        hpBonusPerLevel: Int,
        lastLevelGain: Int?,
    )

    /** the leveled player and the followers in one write, the player's last level gain is kept too */
    suspend fun saveLevelUp(
        player: PartyMember,
        followers: List<PartyMember>,
    )

    /** the members back exactly as given in one write, the approximate flag and the level up numbers included */
    suspend fun restore(members: List<PartyMember>)
}
