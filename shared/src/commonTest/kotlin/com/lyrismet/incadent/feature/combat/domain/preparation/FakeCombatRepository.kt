package com.lyrismet.incadent.feature.combat.domain.preparation

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

internal class FakeCombatRepository : CombatRepository {
    private val combats = MutableStateFlow<List<Combat>>(emptyList())
    private var nextId = 1L

    fun get(id: Long): Combat = combats.value.first { it.id == id }

    override fun observeAll(): Flow<List<Combat>> = combats

    override fun observe(id: Long): Flow<Combat?> = combats.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun create(
        sessionNoteId: Long?,
        startedAt: LocalDateTime,
    ): Long {
        val id = nextId++
        combats.value +=
            Combat(id, sessionNoteId, CombatPhase.PREPARING, startedAt, 1, 0, null, emptyList(), emptyList())
        return id
    }

    override suspend fun addCombatant(
        combatId: Long,
        combatant: Combatant,
    ): Long {
        val id = nextId++
        update(combatId) { it.copy(combatants = it.combatants + combatant.copy(id = id)) }
        return id
    }

    override suspend fun addEvent(
        combatId: Long,
        event: CombatEvent,
    ): Long {
        val id = nextId++
        update(combatId) { it.copy(events = it.events + event.copy(id = id)) }
        return id
    }

    override suspend fun removeCombatant(combatantId: Long) {
        combats.value = combats.value.map { c -> c.copy(combatants = c.combatants.filter { it.id != combatantId }) }
    }

    override suspend fun setTempHp(
        combatantId: Long,
        tempHp: Int,
    ) {
        combats.value =
            combats.value.map { c ->
                c.copy(combatants = c.combatants.map { if (it.id == combatantId) it.copy(tempHp = tempHp) else it })
            }
    }

    override suspend fun renameCombatant(
        combatantId: Long,
        name: String,
    ) {
        combats.value =
            combats.value.map { c ->
                c.copy(combatants = c.combatants.map { if (it.id == combatantId) it.copy(name = name) else it })
            }
    }

    override suspend fun start(
        combatId: Long,
        startedAt: LocalDateTime,
    ) = update(combatId) { it.copy(phase = CombatPhase.ONGOING, startedAt = startedAt, round = 1, turn = 0) }

    override suspend fun setTurn(
        combatId: Long,
        round: Int,
        turn: Int,
    ) = update(combatId) { it.copy(round = round, turn = turn) }

    override suspend fun updateCombatant(combatant: Combatant) {
        combats.value =
            combats.value.map { c ->
                c.copy(
                    combatants =
                        c.combatants.map {
                            if (it.id ==
                                combatant.id
                            ) {
                                combatant
                            } else {
                                it
                            }
                        },
                )
            }
    }

    override suspend fun setTurnOrder(
        combatantId: Long,
        order: Int?,
    ) {
        combats.value =
            combats.value.map { c ->
                c.copy(combatants = c.combatants.map { if (it.id == combatantId) it.copy(turnOrder = order) else it })
            }
    }

    override suspend fun clearTurnOrders(combatId: Long) =
        update(combatId) { c -> c.copy(combatants = c.combatants.map { it.copy(turnOrder = null) }) }

    override suspend fun updateTerm(
        eventId: Long,
        amount: Int,
        effect: HitEffect,
    ) {
        combats.value =
            combats.value.map { c ->
                c.copy(
                    events =
                        c.events.map {
                            if (it.id ==
                                eventId
                            ) {
                                it.copy(amount = amount, effect = effect)
                            } else {
                                it
                            }
                        },
                )
            }
    }

    override suspend fun attachToSession(
        combatId: Long,
        sessionNoteId: Long?,
    ) = update(combatId) { it.copy(sessionNoteId = sessionNoteId) }

    override suspend fun finish(
        combatId: Long,
        outcome: CombatOutcome,
    ) = Unit

    override suspend fun delete(combatId: Long) {
        combats.value = combats.value.filter { it.id != combatId }
    }

    private fun update(
        combatId: Long,
        change: (Combat) -> Combat,
    ) {
        combats.value = combats.value.map { if (it.id == combatId) change(it) else it }
    }
}
