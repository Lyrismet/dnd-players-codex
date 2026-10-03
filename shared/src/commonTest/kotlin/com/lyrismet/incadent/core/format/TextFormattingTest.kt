package com.lyrismet.incadent.core.format

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

    @Test
    fun `joinWithDot joins filled parts with a dot`() {
        assertEquals("Дварф · Гильдия", joinWithDot("Дварф", "Гильдия"))
    }

    @Test
    fun `joinWithDot skips blank parts without a dangling separator`() {
        assertEquals("Дварф", joinWithDot("Дварф", ""))
        assertEquals("Гильдия", joinWithDot("  ", "Гильдия"))
        assertEquals("", joinWithDot("", ""))
    }
}
