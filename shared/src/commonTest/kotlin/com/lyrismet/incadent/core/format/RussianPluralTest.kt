package com.lyrismet.incadent.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class RussianPluralTest {
    private fun forms(count: Int) = russianPluralForm(count, "заметка", "заметки", "заметок")

    @Test
    fun `1 uses the one form`() {
        assertEquals("заметка", forms(1))
    }

    @Test
    fun `2 uses the few form`() {
        assertEquals("заметки", forms(2))
    }

    @Test
    fun `5 uses the many form`() {
        assertEquals("заметок", forms(5))
    }

    @Test
    fun `11 is an exception to one and uses the many form`() {
        assertEquals("заметок", forms(11))
    }

    @Test
    fun `21 ends in 1 and uses the one form again`() {
        assertEquals("заметка", forms(21))
    }

    @Test
    fun `25 uses the many form`() {
        assertEquals("заметок", forms(25))
    }

    @Test
    fun `101 ends in 1 and uses the one form`() {
        assertEquals("заметка", forms(101))
    }

    @Test
    fun `111 falls in the teens exception and uses the many form`() {
        assertEquals("заметок", forms(111))
    }
}
