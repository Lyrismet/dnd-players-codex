package com.lyrismet.incadent.core.portrait

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitSizeKbTest {
    @Test
    fun roundsStoredBytesToKilobytes() {
        assertEquals(60, portraitSizeKb(Base64.Default.encode(ByteArray(60 * 1024))))
        assertEquals(2, portraitSizeKb(Base64.Default.encode(ByteArray(1536))))
        assertEquals(1, portraitSizeKb(Base64.Default.encode(ByteArray(1535))))
    }

    @Test
    fun ignoresBase64Padding() {
        assertEquals(1, portraitSizeKb(Base64.Default.encode(ByteArray(1024 + 1))))
    }
}
