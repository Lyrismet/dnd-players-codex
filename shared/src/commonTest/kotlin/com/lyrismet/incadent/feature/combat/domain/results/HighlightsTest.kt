package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HighlightsTest {
    private val kira = member(1, "Кира", CombatSide.PARTY)
    private val borin = member(2, "Борин", CombatSide.PARTY)
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)

    @Test
    fun `an empty fight has nothing to say`() {
        assertTrue(highlightsOf(combatOf(listOf(kira, goblin))).isEmpty)
    }

    @Test
    fun `top damage is the party member who dealt the most`() {
        val combat = combatOf(listOf(kira, borin, goblin), listOf(strike(1, 1, 3, 5), strike(2, 2, 3, 9)))
        assertEquals("Борин" to 9, highlightsOf(combat).topDamage)
    }

    @Test
    fun `finishers are listed once`() {
        val events = listOf(marker(1, CombatEventKind.DOWN, 1, 3), marker(2, CombatEventKind.DOWN, 1, 3))
        assertEquals(listOf("Кира"), highlightsOf(combatOf(listOf(kira, goblin), events)).finishers)
    }

    @Test
    fun `a party member falling twice is counted`() {
        val events = listOf(marker(1, CombatEventKind.DOWN, 3, 1), marker(2, CombatEventKind.DOWN, 3, 1))
        assertEquals(listOf("Кира" to 2), highlightsOf(combatOf(listOf(kira, goblin), events)).falls)
    }

    @Test
    fun `no damage means no top damage`() {
        assertNull(highlightsOf(combatOf(listOf(kira, goblin))).topDamage)
    }
}
