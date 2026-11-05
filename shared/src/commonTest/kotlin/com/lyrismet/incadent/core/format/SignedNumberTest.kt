package com.lyrismet.incadent.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class SignedNumberTest {
    @Test
    fun `a number shows its sign with a true minus`() {
        assertEquals("+2", 2.signed())
        assertEquals("+0", 0.signed())
        assertEquals("−1", (-1).signed())
    }

    @Test
    fun `a missing number is a dash`() {
        assertEquals("—", null.signed())
    }
}
