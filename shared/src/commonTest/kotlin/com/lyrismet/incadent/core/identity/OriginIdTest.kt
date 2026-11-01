package com.lyrismet.incadent.core.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OriginIdTest {
    @Test
    fun `a new origin id is 32 lowercase hex characters`() {
        val id = newOriginId()

        assertEquals(32, id.length)
        assertTrue(id.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `new origin ids differ`() {
        assertEquals(100, List(100) { newOriginId() }.toSet().size)
    }
}
