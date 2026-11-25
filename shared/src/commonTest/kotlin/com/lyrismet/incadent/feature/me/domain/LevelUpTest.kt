package com.lyrismet.incadent.feature.me.domain

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

    @Test
    fun `a bonus per level catches up over every level taken`() {
        assertEquals(6, member().bonusCatchUp(0, 2))
        assertNull(member().bonusCatchUp(2, 2))
        assertNull(member(hpMax = null).bonusCatchUp(0, 2))
    }
}
