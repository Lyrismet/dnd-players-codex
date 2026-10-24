package com.lyrismet.incadent.feature.combat.domain.grouping

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import kotlin.test.Test
import kotlin.test.assertEquals

class EnemyChipsTest {
    private fun fighter(
        name: String,
        side: CombatSide = CombatSide.ENEMY,
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
    )

    @Test
    fun `numbered enemies fold into one chip with a count`() {
        val chips = listOf(fighter("Скелет-лучник I"), fighter("Скелет-лучник II")).enemyChips()
        assertEquals(listOf(EnemyChip("Скелет-лучник", 2, several = true)), chips)
    }

    @Test
    fun `a single unnumbered enemy is not a group`() {
        assertEquals(
            listOf(EnemyChip("Гоблин-разведчик", 1, several = false)),
            listOf(fighter("Гоблин-разведчик")).enemyChips(),
        )
    }

    @Test
    fun `a lone numbered enemy still reads as a group`() {
        assertEquals(listOf(EnemyChip("Волк", 1, several = true)), listOf(fighter("Волк I")).enemyChips())
    }

    @Test
    fun `the same name twice without numbers is a group`() {
        assertEquals(
            listOf(EnemyChip("Гоблин", 2, several = true)),
            listOf(fighter("Гоблин"), fighter("Гоблин")).enemyChips(),
        )
    }

    @Test
    fun `allies and the party are left out`() {
        val chips = listOf(fighter("Друг", CombatSide.ALLY), fighter("Игрок", CombatSide.PARTY)).enemyChips()
        assertEquals(emptyList(), chips)
    }

    @Test
    fun `chips keep the order of first appearance`() {
        val chips = listOf(fighter("Орк"), fighter("Гоблин"), fighter("Орк")).enemyChips()
        assertEquals(listOf("Орк", "Гоблин"), chips.map { it.name })
    }
}
