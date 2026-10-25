package com.lyrismet.incadent.core.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PhotoKeyTest {
    @Test
    fun `equal bytes give equal keys`() {
        assertEquals(photoKeyOf(byteArrayOf(1, 2, 3)), photoKeyOf(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `same size different bytes give different keys`() {
        assertNotEquals(photoKeyOf(byteArrayOf(1, 2, 3)), photoKeyOf(byteArrayOf(1, 2, 4)))
    }

    @Test
    fun `different size gives different keys`() {
        assertNotEquals(photoKeyOf(byteArrayOf(1, 2, 3)), photoKeyOf(byteArrayOf(1, 2, 3, 0)))
    }
}
