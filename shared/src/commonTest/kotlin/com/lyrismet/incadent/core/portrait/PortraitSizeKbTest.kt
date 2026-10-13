package com.lyrismet.incadent.core.portrait

import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitSizeKbTest {
    @Test
    fun `size is rounded to whole kilobytes`() {
        assertEquals(60, photoSizeKb(ByteArray(60 * 1024)))
        assertEquals(2, photoSizeKb(ByteArray(1536)))
        assertEquals(1, photoSizeKb(ByteArray(1535)))
        assertEquals(1, photoSizeKb(ByteArray(1024 + 1)))
    }
}
