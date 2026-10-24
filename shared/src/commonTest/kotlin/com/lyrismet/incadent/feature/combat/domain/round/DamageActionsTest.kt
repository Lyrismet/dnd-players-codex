package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.damage.DamageTerm
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.damage.planHit
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.preparation.FakeCombatRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DamageActionsTest {
    private val repository = FakeCombatRepository()
    private val actions = DamageActions(repository)
    private val at = LocalDateTime.parse("2026-10-22T20:15")

    private fun member(
        name: String,
        side: CombatSide,
        hp: Int? = null,
        temp: Int = 0,
    ) = Combatant(
        id = 0,
        name = name,
        side = side,
        npcId = null,
        speciesId = null,
        partyMemberId = null,
        enteredWhole = true,
        state = CombatState.WHOLE,
        joinedRound = 1,
        deathSuccesses = 0,
        deathFailures = 0,
        tempHp = temp,
        hpCurrent = hp,
        hpMax = hp,
    )

    private suspend fun fight(): Combat {
        val id = repository.create(null, at)
        repository.addCombatant(id, member("Аэлин", CombatSide.PARTY, hp = 20, temp = 5))
        repository.addCombatant(id, member("Гоблин", CombatSide.ENEMY))
        repository.start(id, at)
        return repository.get(id)
    }

    private fun Combat.named(name: String) = combatants.first { it.name == name }

    private fun strike(
        target: Combatant,
        vararg terms: DamageTerm,
        states: Map<DamageType, DamageState> = emptyMap(),
        scale: HitScale = HitScale.ONE,
    ) = Strike(target, planHit(terms.toList(), scale, states))

    @Test
    fun `damage takes the temporary hit points first`() =
        runTest {
            val combat = fight()
            actions.damage(combat, null, listOf(strike(combat.named("Аэлин"), DamageTerm(8))))
            val after = repository.get(combat.id).named("Аэлин")
            assertEquals(17, after.hpCurrent)
            assertEquals(0, after.tempHp)
        }

    @Test
    fun `every term is told and the terms of one blow share a hit`() =
        runTest {
            val combat = fight()
            actions.damage(
                combat,
                null,
                listOf(strike(combat.named("Аэлин"), DamageTerm(4, DamageType.FIRE), DamageTerm(3))),
            )
            val events = repository.get(combat.id).events.filter { it.kind == CombatEventKind.DAMAGE }
            assertEquals(listOf(4, 3), events.map { it.amount })
            assertEquals(1, events.map { it.hit }.toSet().single())
            assertEquals(listOf(DamageType.FIRE, null), events.map { it.damageType })
        }

    @Test
    fun `falling to zero puts a counted member down`() =
        runTest {
            val combat = fight()
            actions.damage(combat, null, listOf(strike(combat.named("Аэлин"), DamageTerm(99))))
            val after = repository.get(combat.id)
            assertEquals(CombatState.DOWN, after.named("Аэлин").state)
            assertEquals(0, after.named("Аэлин").hpCurrent)
            assertTrue(after.events.any { it.kind == CombatEventKind.DOWN })
        }

    @Test
    fun `the first blow marks a guessed enemy hurt`() =
        runTest {
            val combat = fight()
            actions.damage(combat, null, listOf(strike(combat.named("Гоблин"), DamageTerm(6))))
            assertEquals(CombatState.WOUNDED, repository.get(combat.id).named("Гоблин").state)
        }

    @Test
    fun `a blow that did nothing is not told`() =
        runTest {
            val combat = fight()
            val states = mapOf(DamageType.COLD to DamageState.IMMUNITY)
            actions.damage(
                combat,
                null,
                listOf(strike(combat.named("Гоблин"), DamageTerm(9, DamageType.COLD), states = states)),
            )
            assertTrue(repository.get(combat.id).events.isEmpty())
        }

    @Test
    fun `healing from the ground gets the member up`() =
        runTest {
            val combat = fight()
            actions.damage(combat, null, listOf(strike(combat.named("Аэлин"), DamageTerm(99))))
            val down = repository.get(combat.id)
            actions.heal(down, null, listOf(down.named("Аэлин")), amount = 6)
            val after = repository.get(combat.id).named("Аэлин")
            assertEquals(CombatState.WHOLE, after.state)
            assertEquals(6, after.hpCurrent)
        }

    @Test
    fun `temporary hit points keep the larger number and are told only when they grew`() =
        runTest {
            val combat = fight()
            actions.giveTemp(combat, null, listOf(combat.named("Аэлин")), amount = 3)
            assertEquals(5, repository.get(combat.id).named("Аэлин").tempHp)
            actions.giveTemp(
                repository.get(combat.id),
                null,
                listOf(repository.get(combat.id).named("Аэлин")),
                amount = 9,
            )
            val after = repository.get(combat.id)
            assertEquals(9, after.named("Аэлин").tempHp)
            assertEquals(1, after.events.count { it.kind == CombatEventKind.TEMP })
        }

    @Test
    fun `naming the resistance after the blow gives the hit points back`() =
        runTest {
            val combat = fight()
            val hero = combat.named("Аэлин")
            actions.damage(combat, null, listOf(strike(hero, DamageTerm(12, DamageType.FIRE))))
            val struck = repository.get(combat.id)
            val eventId = struck.events.single().id
            val delta = actions.correct(struck, eventId, HitEffect.HALF)
            val after = repository.get(combat.id)
            assertEquals(-6, delta)
            assertEquals(6, after.events.single().amount)
            assertEquals(HitEffect.HALF, after.events.single().effect)
            assertEquals(19, after.named("Аэлин").hpCurrent)
        }
}
