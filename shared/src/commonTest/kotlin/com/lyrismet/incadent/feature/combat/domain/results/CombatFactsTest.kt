package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CombatFactsTest {
    private val hero = member(1, "Кира", CombatSide.PARTY).copy(partyMemberId = 4)
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)

    private fun fight(
        id: Long,
        session: Long?,
        dealt: Int,
    ) = combatOf(listOf(hero, goblin), listOf(strike(1, 1, 3, dealt))).copy(
        id = id,
        sessionNoteId = session,
        phase = CombatPhase.FINISHED,
    )

    @Test
    fun `the chronicle counts sessions fights and damage`() {
        val facts = combatFacts(listOf(fight(1, 10, 5), fight(2, 10, 7), fight(3, 11, 2)), emptyMap())
        assertEquals(PartyCombatFacts(sessions = 2, fights = 3, damage = 14), facts.party.getValue(4))
    }

    @Test
    fun `a fight that is not finished is not in the chronicle`() {
        val ongoing = fight(1, 10, 5).copy(phase = CombatPhase.ONGOING)
        assertNull(combatFacts(listOf(ongoing), emptyMap()).party[4])
    }

    @Test
    fun `a fall names the session and the foes of its fight`() {
        val fall = combatFacts(listOf(fight(9, 10, 5)), emptyMap()).falls.getValue(9)
        assertEquals(10L, fall.sessionNoteId)
        assertEquals(listOf("Гоблин"), fall.foes.map { it.name })
    }

    @Test
    fun `damage dealt by others is not the members`() {
        val other = fight(1, 10, 5).copy(events = listOf(marker(1, CombatEventKind.DAMAGE, 3, 1).copy(amount = 8)))
        assertEquals(0, combatFacts(listOf(other), emptyMap()).party.getValue(4).damage)
    }
}
