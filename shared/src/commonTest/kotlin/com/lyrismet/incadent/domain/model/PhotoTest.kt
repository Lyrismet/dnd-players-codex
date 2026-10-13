package com.lyrismet.incadent.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhotoTest {
    @Test
    fun `photos with the same bytes are equal`() {
        val a = Photo(byteArrayOf(1, 2), byteArrayOf(3, 4))
        val b = Photo(byteArrayOf(1, 2), byteArrayOf(3, 4))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `a missing full picture differs from a loaded one`() {
        val thumbOnly = Photo(byteArrayOf(1))
        val loaded = Photo(byteArrayOf(1), byteArrayOf(2))

        assertNotEquals(thumbOnly, loaded)
        assertTrue(thumbOnly.display.contentEquals(byteArrayOf(1)))
        assertTrue(loaded.display.contentEquals(byteArrayOf(2)))
    }
}
