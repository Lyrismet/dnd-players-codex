package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpellDamageCodecTest {
    @Test
    fun `rows survive a round trip with their types`() {
        val rows =
            listOf(
                SpellDamage("1d10", SpellEffect.Damage(DamageType.PIERCING)),
                SpellDamage("2d6", SpellEffect.Damage(DamageType.COLD)),
                SpellDamage("5", SpellEffect.TempHp),
                SpellDamage("1d4+3", SpellEffect.Healing),
                SpellDamage("1d8"),
            )
        assertEquals(rows, decodeDamage(encodeDamage(rows)))
    }

    @Test
    fun `separators typed into the dice are dropped`() {
        val back = decodeDamage(encodeDamage(listOf(SpellDamage("1d6;2:d4 x"))))
        assertEquals("1d62d4x", back.single().dice)
        assertNull(back.single().effect)
    }

    @Test
    fun `empty text and unknown types decode safely`() {
        assertTrue(decodeDamage("").isEmpty())
        assertNull(decodeDamage("NOPE:1d6").single().effect)
        assertTrue(decodeDamage("garbage").isEmpty())
    }

    @Test
    fun `dice formats are checked`() {
        listOf("", "1d10", "2d6+3", "5", "1d8+мод", "1D8-1").forEach { assertTrue(isValidDice(it), it) }
        listOf("d", "1d", "abc", "1d6+", "1d6++2").forEach { assertFalse(isValidDice(it), it) }
    }

    @Test
    fun `a spell counts rounds only up to ten minutes`() {
        fun rounds(
            unit: DurationUnit,
            value: Int,
        ) = CharacterSpell(1, "S", 1, durationUnit = unit, durationValue = value).combatRounds
        assertEquals(10, rounds(DurationUnit.MINUTE, 1))
        assertEquals(100, rounds(DurationUnit.MINUTE, 10))
        assertNull(rounds(DurationUnit.MINUTE, 11))
        assertEquals(3, rounds(DurationUnit.ROUND, 3))
        assertNull(rounds(DurationUnit.HOUR, 1))
        assertNull(rounds(DurationUnit.INSTANT, 0))
        assertNull(rounds(DurationUnit.NONE, 0))
    }
}
