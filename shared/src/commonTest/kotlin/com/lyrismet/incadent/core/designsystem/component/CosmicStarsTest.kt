package com.lyrismet.incadent.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CosmicStarsTest {
    private fun layout(seed: Long) = cosmicStars(seed).map { listOf(it.x, it.y, it.radiusDp, it.color.alpha) }

    @Test
    fun `the same seed gives the same sky`() {
        assertEquals(layout(7), layout(7))
    }

    @Test
    fun `neighbouring seeds give different skies`() {
        assertNotEquals(layout(1), layout(2))
        assertNotEquals(layout(2), layout(3))
    }

    @Test
    fun `every star stays inside the card and the count is bounded`() {
        (1L..50L).forEach { seed ->
            val stars = cosmicStars(seed)
            assertTrue(stars.size in 6..10)
            assertTrue(stars.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }
}
