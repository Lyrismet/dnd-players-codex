package com.lyrismet.incadent.feature.combat.domain.repository

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
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

    suspend fun removeCombatant(combatantId: Long)

    suspend fun setTempHp(
        combatantId: Long,
        tempHp: Int,
    )

    suspend fun renameCombatant(
        combatantId: Long,
        name: String,
    )

    /** the preparation is over, the fight starts at [startedAt] with the first participant to move */
    suspend fun start(
        combatId: Long,
        startedAt: LocalDateTime,
    )

    suspend fun setTurn(
        combatId: Long,
        round: Int,
        turn: Int,
    )

    /** writes every field a fight can change on [combatant], the sides and the sources stay as they were */
    suspend fun updateCombatant(combatant: Combatant)

    suspend fun setTurnOrder(
        combatantId: Long,
        order: Int?,
    )

    suspend fun clearTurnOrders(combatId: Long)

    /** a damage term whose resistance was named after the blow gets its new effect and the hit points it really took */
    suspend fun updateTerm(
        eventId: Long,
        amount: Int,
        effect: HitEffect,
    )

    suspend fun attachToSession(
        combatId: Long,
        sessionNoteId: Long?,
    )

    suspend fun finish(
        combatId: Long,
        outcome: CombatOutcome,
        finishedAt: LocalDateTime,
    )

    suspend fun delete(combatId: Long)
}
