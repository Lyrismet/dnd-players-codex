package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpellMarkTest {
    private fun spell(
        concentration: Boolean = false,
        unit: DurationUnit = DurationUnit.NONE,
        value: Int = 0,
    ) = CharacterSpell(1, "S", 1, concentration = concentration, durationUnit = unit, durationValue = value)

    @Test
    fun `a spell with no duration puts no mark`() {
        assertNull(spell().markFor(circle = 1, characterLevel = 1))
    }

    @Test
    fun `a lasting spell puts a counted spell mark`() {
        assertEquals(
            SpellMark(MarkKind.SPELL, 10),
            spell(unit = DurationUnit.MINUTE, value = 1).markFor(circle = 1, characterLevel = 1),
        )
    }

    @Test
    fun `a concentration spell always puts a concentration mark`() {
        assertEquals(
            SpellMark(MarkKind.CONCENTRATION, null),
            spell(concentration = true).markFor(circle = 1, characterLevel = 1),
        )
    }
}
