package com.lyrismet.incadent.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class ThousandsTest {
    @Test
    fun `small numbers have no space`() {
        assertEquals("0", 0.withThousands())
        assertEquals("999", 999.withThousands())
    }

    @Test
    fun `thousands are split by a no break space`() {
        assertEquals("1 000", 1000.withThousands())
        assertEquals("9 999 999", 9_999_999.withThousands())
        assertEquals("12 345", 12_345.withThousands())
    }
}
