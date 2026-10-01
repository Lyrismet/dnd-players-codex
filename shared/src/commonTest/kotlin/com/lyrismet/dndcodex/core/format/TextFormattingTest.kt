package com.lyrismet.dndcodex.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class TextFormattingTest {
    @Test
    fun `capitalizeFirst uppercases an all-lowercase name`() {
        assertEquals("Громм", "громм".capitalizeFirst())
    }

    @Test
    fun `capitalizeFirst leaves an already-capitalized name untouched`() {
        assertEquals("Громм", "Громм".capitalizeFirst())
    }

    @Test
    fun `capitalizeFirst only touches the first character`() {
        assertEquals("Брат одо", "брат одо".capitalizeFirst())
    }

    @Test
    fun `capitalizeFirst on an empty string stays empty`() {
        assertEquals("", "".capitalizeFirst())
    }
}
