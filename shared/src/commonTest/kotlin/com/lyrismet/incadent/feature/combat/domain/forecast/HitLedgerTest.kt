package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.preparation.FakeCombatRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HitLedgerTest {
    private val repository = FakeCombatRepository()

    private fun event(
        kind: CombatEventKind,
        amount: Int = 0,
        state: CombatState? = null,
        raw: Int = 0,
    ) = CombatEvent(0, 1, kind, null, 7, amount, null, HitEffect.FULL, state, raw = raw)

    private suspend fun ledgerOf(vararg told: CombatEvent): HitLedger {
        val id = repository.create(null, LocalDateTime.parse("2026-10-22T20:15"))
        told.forEach { repository.addEvent(id, it) }
        return repository.get(id).ledgerOf(7)
    }

    @Test
    fun `damage and healing add up and the net never goes below zero`() =
        runTest {
            val ledger = ledgerOf(event(CombatEventKind.DAMAGE, 10), event(CombatEventKind.HEAL, 15))
            assertEquals(10, ledger.damage)
            assertEquals(15, ledger.healed)
            assertEquals(0, ledger.net)
        }

    @Test
    fun `a mark keeps the healing told before it and no scale place as none`() =
        runTest {
            val ledger =
                ledgerOf(
                    event(CombatEventKind.HEAL, 4),
                    event(CombatEventKind.STATE, 12, CombatState.BLOODIED),
                    event(CombatEventKind.STATE, 12, CombatState.WOUNDED, raw = 600),
                )
            assertEquals(
                listOf(StateMark(CombatState.BLOODIED, 12, 4, null), StateMark(CombatState.WOUNDED, 12, 4, 600)),
                ledger.marks,
            )
        }

    @Test
    fun `typed hit points count the healing after them and a later mark drops them`() =
        runTest {
            val typed =
                ledgerOf(
                    event(CombatEventKind.DAMAGE, 8),
                    event(CombatEventKind.SET, 5),
                    event(CombatEventKind.HEAL, 3),
                )
            assertEquals(ExactMark(5, 8, 0), typed.exact)
            assertEquals(3, typed.healedSinceSet)
            val dropped = ledgerOf(event(CombatEventKind.SET, 5), event(CombatEventKind.STATE, 8, CombatState.BLOODIED))
            assertNull(dropped.exact)
        }
}
