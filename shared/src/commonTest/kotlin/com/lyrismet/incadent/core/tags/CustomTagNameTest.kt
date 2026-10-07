package com.lyrismet.incadent.core.tags

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomTagNameTest {
    @Test
    fun `an empty draft normalizes to null`() {
        assertNull(normalizeCustomTagName(""))
    }

    @Test
    fun `a whitespace only draft normalizes to null`() {
        assertNull(normalizeCustomTagName("   "))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("Засада", normalizeCustomTagName("  засада  "))
    }

    @Test
    fun `the first letter is capitalized`() {
        assertEquals("Торговля", normalizeCustomTagName("торговля"))
    }

    @Test
    fun `an already capitalized draft is unchanged`() {
        assertEquals("Квест", normalizeCustomTagName("Квест"))
    }

    @Test
    fun `a non letter first character is left as is`() {
        assertEquals("200 зм", normalizeCustomTagName("200 зм"))
    }
}
