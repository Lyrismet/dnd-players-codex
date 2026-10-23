package com.lyrismet.incadent.feature.combat.data

import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombatRepositoryTest {
    private val db = TestDatabase()
    private val repository = CombatRepositoryImpl(db.database, db.current)

    private companion object {
        val STARTED_AT = LocalDateTime.parse("2026-10-22T20:15")
    }

    private fun goblin() =
        Combatant(
            id = 0,
            name = "Гоблин",
            side = CombatSide.ENEMY,
            npcId = null,
            speciesId = null,
            partyMemberId = null,
            enteredWhole = true,
            state = CombatState.WHOLE,
            joinedRound = 1,
            deathSuccesses = 0,
            deathFailures = 0,
        )

    @Test
    fun `a new combat starts at round one without an outcome`() =
        runTest {
            val id = repository.create(sessionNoteId = null, startedAt = STARTED_AT)
            val combat = assertNotNull(repository.observe(id).first())
            assertEquals(1, combat.round)
            assertEquals(CombatPhase.PREPARING, combat.phase)
            assertEquals(STARTED_AT, combat.startedAt)
            assertNull(combat.outcome)
        }

    @Test
    fun `combatants and events round trip`() =
        runTest {
            val combatId = repository.create(null, STARTED_AT)
            val targetId = repository.addCombatant(combatId, goblin())
            repository.addEvent(
                combatId,
                CombatEvent(0, 1, CombatEventKind.DAMAGE, null, targetId, 7, DamageType.FIRE, HitEffect.HALF, null),
            )
            val stored =
                repository
                    .observe(combatId)
                    .first()!!
                    .combatants
                    .single()
            repository.updateCombatant(
                stored.copy(
                    state = CombatState.BLOODIED,
                    deathSuccesses = 2,
                    deathFailures = 1,
                    conditions = setOf(CombatCondition.PRONE),
                    note = "хромает",
                ),
            )
            repository.setTurn(combatId, 3, 1)
            repository.finish(combatId, CombatOutcome.RETREAT, STARTED_AT)

            val combat = assertNotNull(repository.observe(combatId).first())
            assertEquals(3, combat.round)
            assertEquals(CombatOutcome.RETREAT, combat.outcome)
            assertEquals(CombatPhase.FINISHED, combat.phase)
            assertEquals(1, combat.turn)
            val member = combat.combatants.single()
            assertEquals(CombatState.BLOODIED, member.state)
            assertEquals(setOf(CombatCondition.PRONE), member.conditions)
            assertEquals("хромает", member.note)
            assertEquals(2, member.deathSuccesses)
            val event = combat.events.single()
            assertEquals(DamageType.FIRE, event.damageType)
            assertEquals(HitEffect.HALF, event.effect)
            assertEquals(7, event.amount)
        }

    @Test
    fun `deleting a combat removes its combatants and events`() =
        runTest {
            val combatId = repository.create(null, STARTED_AT)
            val targetId = repository.addCombatant(combatId, goblin())
            repository.addEvent(
                combatId,
                CombatEvent(0, 1, CombatEventKind.HEAL, null, targetId, 3, null, HitEffect.FULL, null),
            )
            repository.delete(combatId)
            assertTrue(repository.observeAll().first().isEmpty())
            assertNull(repository.observe(combatId).first())
        }

    @Test
    fun `a damage term keeps its raw number and can be recounted`() =
        runTest {
            val combatId = repository.create(null, STARTED_AT)
            val targetId = repository.addCombatant(combatId, goblin())
            val event =
                CombatEvent(
                    0,
                    1,
                    CombatEventKind.DAMAGE,
                    null,
                    targetId,
                    12,
                    DamageType.FIRE,
                    HitEffect.FULL,
                    null,
                    hit = 1,
                    raw = 12,
                )
            val eventId = repository.addEvent(combatId, event)
            repository.updateTerm(eventId, 6, HitEffect.HALF)
            val stored = assertNotNull(repository.observe(combatId).first()).events.single()
            assertEquals(12, stored.raw)
            assertEquals(6, stored.amount)
            assertEquals(HitEffect.HALF, stored.effect)
            assertEquals(1, stored.hit)
        }

    @Test
    fun `combats are scoped to the open campaign`() =
        runTest {
            val id = repository.create(null, STARTED_AT)
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)
            assertTrue(repository.observeAll().first().isEmpty())
            assertNull(repository.observe(id).first())
        }
}
