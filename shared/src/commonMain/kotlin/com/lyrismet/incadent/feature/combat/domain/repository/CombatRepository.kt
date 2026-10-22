package com.lyrismet.incadent.feature.combat.domain.repository

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface CombatRepository {
    fun observeAll(): Flow<List<Combat>>

    fun observe(id: Long): Flow<Combat?>

    /** a new combat starts in preparation */
    suspend fun create(
        sessionNoteId: Long?,
        startedAt: LocalDateTime,
    ): Long

    /** the id on [combatant] is ignored, the stored one is returned */
    suspend fun addCombatant(
        combatId: Long,
        combatant: Combatant,
    ): Long

    /** the id on [event] is ignored, the stored one is returned */
    suspend fun addEvent(
        combatId: Long,
        event: CombatEvent,
    ): Long

    suspend fun removeEvent(eventId: Long)

    suspend fun setState(
        combatantId: Long,
        state: CombatState,
    )

    suspend fun setDeathSaves(
        combatantId: Long,
        successes: Int,
        failures: Int,
    )

    suspend fun setRound(
        combatId: Long,
        round: Int,
    )

    suspend fun finish(
        combatId: Long,
        outcome: CombatOutcome,
    )

    suspend fun delete(combatId: Long)
}
