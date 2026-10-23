package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundLogTest {
    private val kira = member(1, "Кира", CombatSide.PARTY)
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)

    @Test
    fun `the terms of one blow are one line with the total`() {
        val events = listOf(strike(1, 1, 3, 5, hit = 1), strike(2, 1, 3, 4, hit = 1))
        val log = roundLog(combatOf(listOf(kira, goblin), events))
        assertEquals(listOf(LogEntry.Damage("Кира", "Гоблин", 9)), log.map { it.entry })
    }

    @Test
    fun `a fall names who finished it`() {
        val log = roundLog(combatOf(listOf(kira, goblin), listOf(marker(1, CombatEventKind.DOWN, 1, 3))))
        assertEquals(listOf(LogEntry.Down("Гоблин", party = false, by = "Кира")), log.map { it.entry })
    }

    @Test
    fun `a state mark is part of the log`() {
        val mark =
            marker(
                1,
                CombatEventKind.STATE,
                null,
                3,
            ).copy(state = com.lyrismet.incadent.feature.combat.domain.model.CombatState.BLOODIED)
        assertEquals(1, roundLog(combatOf(listOf(kira, goblin), listOf(mark))).size)
    }

    @Test
    fun `conditions stay out of the log`() {
        val condition = marker(1, CombatEventKind.CONDITION, null, 3)
        assertEquals(0, roundLog(combatOf(listOf(kira, goblin), listOf(condition))).size)
    }
}
