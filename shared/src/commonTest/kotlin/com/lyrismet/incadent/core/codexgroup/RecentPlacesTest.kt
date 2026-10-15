package com.lyrismet.incadent.core.codexgroup

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentPlacesTest {
    @Test
    fun `the places picked last come first and none repeats`() {
        assertEquals(listOf(3L, 1L, 2L), listOf(1L, 2L, 3L).withRecentPlaces(listOf(3L)))
    }

    @Test
    fun `only the latest dozen are kept`() {
        val many = (1L..12L).toList()

        assertEquals(listOf(99L) + (1L..11L).toList(), many.withRecentPlaces(listOf(99L)))
    }
}
