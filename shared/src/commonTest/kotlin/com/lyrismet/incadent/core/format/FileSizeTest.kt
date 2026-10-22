package com.lyrismet.incadent.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class FileSizeTest {
    @Test
    fun `under a megabyte shows whole kilobytes and at least one`() {
        assertEquals(FileSize("1", false), fileSizeOf(0, ','))
        assertEquals(FileSize("1", false), fileSizeOf(300, ','))
        assertEquals(FileSize("12", false), fileSizeOf(12 * 1024L, ','))
    }

    @Test
    fun `a megabyte and over shows one decimal with the given separator`() {
        assertEquals(FileSize("1,5", true), fileSizeOf(1_572_864, ','))
        assertEquals(FileSize("2.0", true), fileSizeOf(2L * 1024 * 1024, '.'))
    }
}
