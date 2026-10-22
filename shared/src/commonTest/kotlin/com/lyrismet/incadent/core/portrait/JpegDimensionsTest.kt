package com.lyrismet.incadent.core.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JpegDimensionsTest {
    private val header = byteArrayOf(-1, -40, -1, -64, 0, 17, 8, 0, 32, 0, 16, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1, -1, -39)

    @Test
    fun `the frame header gives width and height`() {
        assertEquals(16 to 32, jpegDimensions(header))
    }

    @Test
    fun `a segment before the frame is skipped`() {
        val app = byteArrayOf(-1, -32, 0, 4, 0, 0)
        val bytes = header.copyOfRange(0, 2) + app + header.copyOfRange(2, header.size)
        assertEquals(16 to 32, jpegDimensions(bytes))
    }

    @Test
    fun `bytes that are not a jpeg give nothing`() {
        assertNull(jpegDimensions(byteArrayOf(1, 2, 3)))
        assertNull(jpegDimensions(ByteArray(0)))
        assertNull(jpegDimensions(header.copyOfRange(0, 6)))
    }

    @Test
    fun `a zero sized frame gives nothing`() {
        val bytes = header.copyOf().also { it[8] = 0 }
        assertNull(jpegDimensions(bytes))
    }
}
