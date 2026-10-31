package com.lyrismet.incadent.core.appinfo

import kotlin.test.Test
import kotlin.test.assertEquals

class AppVersionTest {
    @Test
    fun `a stamped name and code are kept as they are`() {
        assertEquals(AppVersion("9.0.0", 90000), AppVersion.of("9.0.0", "90000"))
    }

    @Test
    fun `spaces around the stamped values are dropped`() {
        assertEquals(AppVersion("0.9.0", 900), AppVersion.of(" 0.9.0 ", " 900 "))
    }

    @Test
    fun `a missing or blank name falls back to a zero version`() {
        assertEquals("0.0.0", AppVersion.of(null, "1").name)
        assertEquals("0.0.0", AppVersion.of("  ", "1").name)
    }

    @Test
    fun `a missing or broken code falls back to zero`() {
        assertEquals(0, AppVersion.of("1.0.0", null).code)
        assertEquals(0, AppVersion.of("1.0.0", "abc").code)
    }
}
