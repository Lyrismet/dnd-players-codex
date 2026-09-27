package com.lyrismet.dndcodex.core.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RomanNumeralsTest {
    @Test
    fun `toRomanNumeral converts simple values`() {
        assertEquals("I", 1.toRomanNumeral())
        assertEquals("III", 3.toRomanNumeral())
        assertEquals("V", 5.toRomanNumeral())
        assertEquals("X", 10.toRomanNumeral())
    }

    @Test
    fun `toRomanNumeral applies subtractive notation`() {
        assertEquals("IV", 4.toRomanNumeral())
        assertEquals("IX", 9.toRomanNumeral())
        assertEquals("XIV", 14.toRomanNumeral())
        assertEquals("XL", 40.toRomanNumeral())
        assertEquals("XC", 90.toRomanNumeral())
        assertEquals("CD", 400.toRomanNumeral())
        assertEquals("CM", 900.toRomanNumeral())
    }

    @Test
    fun `toRomanNumeral handles multi-digit combinations`() {
        assertEquals("MCMXCIV", 1994.toRomanNumeral())
        assertEquals("MMMCMXCIX", 3999.toRomanNumeral())
    }

    @Test
    fun `toRomanNumeral rejects zero and negative numbers`() {
        assertFailsWith<IllegalArgumentException> { 0.toRomanNumeral() }
        assertFailsWith<IllegalArgumentException> { (-5).toRomanNumeral() }
    }
}
