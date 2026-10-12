package com.lyrismet.incadent.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DashTest {
    @Test
    fun `a set number is shown as it is and an unset one as a dash`() {
        assertEquals("12", 12.orDash())
        assertEquals("—", (null as Int?).orDash())
    }

    @Test
    fun `blank text is shown as a dash`() {
        assertEquals("—", "  ".orDash())
        assertEquals("—", (null as String?).orDash())
        assertEquals("Серебро", "Серебро".orDash())
    }
}
