package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceRulesTest {
    private val slots = CharacterResource(1, "Slots", max = 2, current = 2, slotLevel = 1)
    private val rage = CharacterResource(2, "Rage", max = 3, current = 3)

    @Test
    fun `the spell tab is shown unless the player hid it`() {
        assertTrue(isSpellTabVisible(CharacterSheet()))
        assertTrue(isSpellTabVisible(CharacterSheet(spellAbility = Ability.WISDOM)))
        assertFalse(isSpellTabVisible(CharacterSheet(spellsHidden = true)))
    }

    @Test
    fun `a resource is held between zero and its max`() {
        assertEquals(3, rage.copy(current = 9).clamped().current)
        assertEquals(0, rage.copy(current = -1).clamped().current)
        assertEquals(99, rage.copy(max = 500).clamped().max)
        assertEquals(9, slots.copy(slotLevel = 12).clamped().slotLevel)
    }
}
