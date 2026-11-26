package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.grouping.withClasses
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LevelUpTest {
    private fun member(
        level: Int? = 3,
        hpMax: Int? = 24,
        hpCurrent: Int? = 20,
    ) = PartyMember(
        id = 1,
        name = "Arianna",
        characterClass = "",
        race = "",
        level = level,
        playerName = "",
        isPlayerCharacter = true,
        presence = PartyPresence.IN,
        hpMax = hpMax,
        hpCurrent = hpCurrent,
        description = "",
    )

    @Test
    fun `a level up adds the gain to the maximum and to the hit points left`() {
        val after = checkNotNull(member().leveledUp(7))
        assertEquals(4, after.level)
        assertEquals(31, after.hpMax)
        assertEquals(27, after.hpCurrent)
        assertEquals(7, after.lastLevelGain)
    }

    @Test
    fun `several levels add the gain for each and stop at the top level`() {
        val after = checkNotNull(member().leveledUp(5, levels = 3))
        assertEquals(6, after.level)
        assertEquals(39, after.hpMax)
        assertEquals(5, after.lastLevelGain)
        assertEquals(20, checkNotNull(member(level = 18).leveledUp(1, levels = 9)).level)
    }

    @Test
    fun `a level up without a maximum changes only the level`() {
        val after = checkNotNull(member(hpMax = null, hpCurrent = null).leveledUp(7))
        assertEquals(4, after.level)
        assertNull(after.hpMax)
    }

    @Test
    fun `the top level cannot go higher`() {
        assertFalse(member(level = 20).canLevelUp)
        assertNull(member(level = 20).leveledUp(5))
    }

    private val cleric = PartyClass("Жрец", "Домен Света", 3)
    private val fighter = PartyClass("Воин", "", 2)

    @Test
    fun `levels of a multiclass character go to the classes the player chose`() {
        val two = member(level = 5).withClasses(listOf(cleric, fighter))
        val after = checkNotNull(two.leveledUp(6, levels = 3, perClass = listOf(1, 2)))
        assertEquals(listOf(PartyClass("Жрец", "Домен Света", 4), PartyClass("Воин", "", 4)), after.classes)
        assertEquals(8, after.level)
        assertEquals(42, after.hpMax)
    }

    @Test
    fun `a split that does not add up to the levels taken is ignored`() {
        val two = member(level = 5).withClasses(listOf(cleric, fighter))
        val after = checkNotNull(two.leveledUp(6, levels = 2, perClass = listOf(1, 2)))
        assertEquals(listOf(cleric, fighter), after.classes)
        assertEquals(7, after.level)
    }
}
