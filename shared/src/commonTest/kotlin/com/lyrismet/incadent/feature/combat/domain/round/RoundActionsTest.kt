package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.preparation.FakeCombatRepository
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource
import com.lyrismet.incadent.feature.combat.domain.preparation.PartyHp
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundActionsTest {
    private val repository = FakeCombatRepository()
    private val actions = RoundActions(repository)
    private val at = LocalDateTime.parse("2026-10-22T20:15")

    private fun newMember(
        name: String,
        side: CombatSide,
        order: Int,
        partyMemberId: Long? = null,
    ) = Combatant(
        id = 0,
        name = name,
        side = side,
        npcId = null,
        speciesId = null,
        partyMemberId = partyMemberId,
        enteredWhole = true,
        state = CombatState.WHOLE,
        joinedRound = 1,
        deathSuccesses = 0,
        deathFailures = 0,
        turnOrder = order,
    )

    /** a fight of a party member, an ally and two enemies, in this order */
    private suspend fun fight(): Combat {
        val id = repository.create(null, at)
        repository.addCombatant(id, newMember("Аэлин", CombatSide.PARTY, 1, partyMemberId = 7))
        repository.addCombatant(id, newMember("Фамильяр", CombatSide.ALLY, 2))
        repository.addCombatant(id, newMember("Гоблин I", CombatSide.ENEMY, 3))
        repository.addCombatant(id, newMember("Гоблин II", CombatSide.ENEMY, 4))
        repository.start(id, at)
        return repository.get(id)
    }

    private fun Combat.named(name: String) = combatants.first { it.name == name }

    @Test
    fun `beginning the fight follows the own hit points by numbers`() =
        runTest {
            val id = repository.create(null, at)
            repository.addCombatant(id, newMember("Аэлин", CombatSide.PARTY, 1, partyMemberId = 7))
            actions.begin(repository.get(id), at, mapOf(7L to PartyHp(current = 20, max = 30)))
            val combat = repository.get(id)
            assertEquals(CombatPhase.ONGOING, combat.phase)
            assertEquals(30, combat.named("Аэлин").hpMax)
            assertEquals(20, combat.named("Аэлин").hpCurrent)
        }

    @Test
    fun `a mark is kept and told with the damage taken so far`() =
        runTest {
            val combat = fight()
            actions.setMark(combat, combat.named("Гоблин I"), CombatState.BLOODIED)
            val after = repository.get(combat.id)
            assertEquals(CombatState.BLOODIED, after.named("Гоблин I").state)
            assertEquals(CombatEventKind.STATE, after.events.single().kind)
        }

    @Test
    fun `going down is told and zeroes known hit points`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин II")
            repository.updateCombatant(goblin.copy(hpCurrent = 5, hpMax = 7))
            actions.setDown(repository.get(combat.id), repository.get(combat.id).named("Гоблин II"))
            val after = repository.get(combat.id).named("Гоблин II")
            assertEquals(CombatState.DOWN, after.state)
            assertEquals(0, after.hpCurrent)
        }

    @Test
    fun `the enemy whose turn it is hands the turn on when it goes down`() =
        runTest {
            val combat = fight()
            repository.setTurn(combat.id, 1, 2)
            val now = repository.get(combat.id)
            actions.setDown(now, now.named("Гоблин I"))
            assertEquals(3, repository.get(combat.id).turn)
        }

    @Test
    fun `standing up brings an unnumbered one back barely standing and a numbered one to one hit point`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин I")
            actions.setDown(combat, goblin)
            actions.standUp(repository.get(combat.id), repository.get(combat.id).named("Гоблин I"))
            assertEquals(CombatState.BARELY_STANDING, repository.get(combat.id).named("Гоблин I").state)
            val ally = repository.get(combat.id).named("Фамильяр")
            repository.updateCombatant(ally.copy(hpCurrent = 0, hpMax = 6, state = CombatState.DOWN))
            actions.standUp(repository.get(combat.id), repository.get(combat.id).named("Фамильяр"))
            assertEquals(1, repository.get(combat.id).named("Фамильяр").hpCurrent)
        }

    @Test
    fun `the next turn gives the own reaction back`() =
        runTest {
            val combat = fight()
            val own = combat.named("Аэлин")
            repository.updateCombatant(own.copy(reactionAvailable = false))
            repository.setTurn(combat.id, 1, 3)
            actions.next(repository.get(combat.id), ownCharacterIds = setOf(own.id))
            val after = repository.get(combat.id)
            assertEquals(2, after.round)
            assertEquals(0, after.turn)
            assertTrue(after.named("Аэлин").reactionAvailable)
        }

    @Test
    fun `removing someone keeps the turn on the same one`() =
        runTest {
            val combat = fight()
            repository.setTurn(combat.id, 1, 2)
            actions.remove(repository.get(combat.id), combat.named("Аэлин"))
            assertEquals(1, repository.get(combat.id).turn)
        }

    @Test
    fun `a removed one comes back with its state and the blows it took`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин I")
            repository.updateCombatant(
                goblin.copy(state = CombatState.WOUNDED, note = "хромает", hpCurrent = 4, hpMax = 7),
            )
            repository.addEvent(
                combat.id,
                CombatEvent(0, 1, CombatEventKind.DAMAGE, null, goblin.id, 3, null, HitEffect.FULL, null),
            )
            val removed = actions.remove(repository.get(combat.id), repository.get(combat.id).named("Гоблин I"))
            assertEquals(0, repository.get(combat.id).events.size)
            actions.restore(checkNotNull(removed), repository.get(combat.id))
            val after = repository.get(combat.id)
            val back = after.named("Гоблин I")
            assertEquals(CombatState.WOUNDED, back.state)
            assertEquals("хромает", back.note)
            assertEquals(4, back.hpCurrent)
            assertEquals(listOf(back.id), after.events.map { it.targetId })
        }

    @Test
    fun `restoring brings the turn back only while the fight stands where the removal left it`() =
        runTest {
            val combat = fight()
            repository.setTurn(combat.id, 1, 2)
            val removed = checkNotNull(actions.remove(repository.get(combat.id), combat.named("Аэлин")))
            actions.restore(removed, repository.get(combat.id))
            assertEquals(2, repository.get(combat.id).turn)

            val again =
                checkNotNull(actions.remove(repository.get(combat.id), repository.get(combat.id).named("Аэлин")))
            repository.setTurn(combat.id, 2, 0)
            actions.restore(again, repository.get(combat.id))
            assertEquals(0, repository.get(combat.id).turn)
        }

    @Test
    fun `the last one cannot be removed so there is nothing to restore`() =
        runTest {
            val id = repository.create(null, at)
            repository.addCombatant(id, newMember("Один", CombatSide.ENEMY, 1))
            repository.start(id, at)
            assertEquals(null, actions.remove(repository.get(id), repository.get(id).named("Один")))
        }

    @Test
    fun `a reinforcement moves last and numbers its kind`() =
        runTest {
            val combat = fight()
            actions.reinforce(combat, CombatSide.ENEMY, MemberSource(null, null, "Гоблин"))
            val after = repository.get(combat.id)
            assertEquals(
                listOf("Гоблин I", "Гоблин II", "Гоблин III"),
                after.combatants
                    .filter {
                        it.name.startsWith("Гоблин")
                    }.map { it.name },
            )
            assertEquals(5, after.combatants.last().turnOrder)
            assertEquals(CombatEventKind.JOINED, after.events.last().kind)
        }

    @Test
    fun `a condition is told when it goes on and when it goes off`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин I")
            actions.toggleCondition(combat, goblin, CombatCondition.PRONE)
            val on = repository.get(combat.id)
            assertTrue(CombatCondition.PRONE in on.named("Гоблин I").conditions)
            actions.toggleCondition(on, on.named("Гоблин I"), CombatCondition.PRONE)
            val off = repository.get(combat.id)
            assertFalse(CombatCondition.PRONE in off.named("Гоблин I").conditions)
            assertEquals(listOf("+PRONE", "-PRONE"), off.events.map { it.detail })
        }

    @Test
    fun `a stance change is told once`() =
        runTest {
            val combat = fight()
            val goblin = combat.named("Гоблин I")
            actions.setStance(combat, goblin, CombatStance.FRIEND)
            actions.setStance(
                repository.get(combat.id),
                repository.get(combat.id).named("Гоблин I"),
                CombatStance.FRIEND,
            )
            assertEquals(1, repository.get(combat.id).events.size)
        }
}
