package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HitObservationsTest {
    private val kira = member(1, "Кира", CombatSide.PARTY)
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)

    @Test
    fun `a fallen enemy remembers the damage at the first fall`() {
        val events =
            listOf(strike(1, 1, 3, 5), strike(2, 1, 3, 4), marker(3, CombatEventKind.DOWN, 1, 3), strike(4, 1, 3, 6))
        val draft = hitObservations(combatOf(listOf(kira, goblin), events)).single()
        assertEquals(9, draft.damage)
        assertTrue(draft.fell)
    }

    @Test
    fun `a survivor remembers the damage it took`() {
        val draft = hitObservations(combatOf(listOf(kira, goblin), listOf(strike(1, 1, 3, 7)))).single()
        assertEquals(7, draft.damage)
        assertEquals(false, draft.fell)
    }

    @Test
    fun `healing lowers the damage taken`() {
        val events = listOf(strike(1, 1, 3, 10), healEvent(2, 3, 4), strike(3, 1, 3, 2))
        assertEquals(8, hitObservations(combatOf(listOf(kira, goblin), events)).single().damage)
    }

    @Test
    fun `an enemy that did not enter whole says nothing`() {
        val hurt = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7, enteredWhole = false)
        assertTrue(hitObservations(combatOf(listOf(kira, hurt), listOf(strike(1, 1, 3, 7)))).isEmpty())
    }

    @Test
    fun `an enemy without a species says nothing`() {
        val named = member(3, "Вожак", CombatSide.ENEMY, npcId = 5)
        assertTrue(hitObservations(combatOf(listOf(kira, named), listOf(strike(1, 1, 3, 7)))).isEmpty())
    }

    @Test
    fun `an untouched enemy says nothing`() {
        assertTrue(hitObservations(combatOf(listOf(kira, goblin))).isEmpty())
    }
}

private fun healEvent(
    id: Long,
    target: Long,
    amount: Int,
) = marker(id, CombatEventKind.HEAL, null, target).copy(amount = amount)
