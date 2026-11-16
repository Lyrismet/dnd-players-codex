package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.damage.DamageTerm
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.damage.missingHp
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

    private suspend fun hurtFight(): Combat {
        val combat = fight()
        actions.damage(combat, null, listOf(strike(combat.named("Аэлин"), DamageTerm(9))))
        return repository.get(combat.id)
    }

    @Test
    fun `heal to maximum fills the hit points and leaves the temporary ones`() =
        runTest {
            val combat = hurtFight()
            val hero = combat.named("Аэлин")
            assertEquals(16, hero.hpCurrent)
            val healed = actions.healToMax(combat, null, listOf(hero))
            val after = repository.get(combat.id).named("Аэлин")
            assertEquals(20, after.hpCurrent)
            assertEquals(hero.tempHp, after.tempHp)
            assertEquals(1, healed?.before?.size)
        }

    @Test
    fun `heal to maximum of several targets tells one heal each`() =
        runTest {
            val combat = hurtFight()
            val id = combat.id
            repository.addCombatant(id, member("Брон", CombatSide.PARTY, hp = 30).copy(hpCurrent = 10))
            val fresh = repository.get(id)
            val targets = fresh.combatants.filter { it.side == CombatSide.PARTY }
            actions.healToMax(fresh, null, targets)
            val after = repository.get(id)
            assertEquals(20, after.named("Аэлин").hpCurrent)
            assertEquals(30, after.named("Брон").hpCurrent)
            assertEquals(2, after.events.count { it.kind == CombatEventKind.HEAL })
        }

    @Test
    fun `heal to maximum skips full and unknown targets`() =
        runTest {
            val combat = fight()
            val targets = combat.combatants
            assertEquals(null, actions.healToMax(combat, null, targets))
            assertEquals(0, repository.get(combat.id).events.size)
        }

    @Test
    fun `heal to maximum brings a downed target back on its feet`() =
        runTest {
            val combat = fight()
            actions.damage(combat, null, listOf(strike(combat.named("Аэлин"), DamageTerm(40))))
            val down = repository.get(combat.id).named("Аэлин")
            assertEquals(CombatState.DOWN, down.state)
            actions.healToMax(repository.get(combat.id), null, listOf(down))
            val up = repository.get(combat.id).named("Аэлин")
            assertEquals(20, up.hpCurrent)
            assertEquals(CombatState.WHOLE, up.state)
        }

    @Test
    fun `one undo takes back the heal to maximum of every target`() =
        runTest {
            val combat = hurtFight()
            val before = repository.get(combat.id)
            val healed = checkNotNull(actions.healToMax(before, null, before.combatants.filter { it.hpMax != null }))
            actions.undo(healed)
            val after = repository.get(combat.id)
            assertEquals(before.named("Аэлин").hpCurrent, after.named("Аэлин").hpCurrent)
            assertEquals(before.events.size, after.events.size)
        }

    @Test
    fun `missing hit points are none while the maximum is a guess`() {
        assertEquals(null, member("Гоблин", CombatSide.ENEMY).missingHp())
        assertEquals(0, member("Аэлин", CombatSide.PARTY, hp = 20).missingHp())
        assertEquals(6, member("Аэлин", CombatSide.PARTY, hp = 20).copy(hpCurrent = 14).missingHp())
    }

    @Test
    fun `typed hit points of a guessed target are told as one event and move its word`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин")
            actions.damage(combat, null, listOf(strike(goblin, DamageTerm(40))))
            val struck = repository.get(combat.id)
            actions.setExact(struck, null, struck.named("Гоблин"), amount = 1)
            val after = repository.get(combat.id)
            assertEquals(1, after.events.single { it.kind == CombatEventKind.SET }.amount)
            assertEquals(CombatState.BARELY_STANDING, after.named("Гоблин").state)
        }

    @Test
    fun `typed hit points above the told healing can add the missing heal and one undo takes both`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин")
            val recorded = actions.setExact(combat, null, goblin, amount = 33, unrecordedHeal = 15)
            val after = repository.get(combat.id)
            assertEquals(15, after.events.single { it.kind == CombatEventKind.HEAL }.amount)
            assertEquals(2, recorded.eventIds.size)
            actions.undo(recorded)
            assertEquals(0, repository.get(combat.id).events.size)
        }

    @Test
    fun `typed hit points of a counted target set its hit points within the maximum`() =
        runTest {
            val combat = fight()
            actions.setExact(combat, null, combat.named("Аэлин"), amount = 99)
            assertEquals(20, repository.get(combat.id).named("Аэлин").hpCurrent)
        }
}
