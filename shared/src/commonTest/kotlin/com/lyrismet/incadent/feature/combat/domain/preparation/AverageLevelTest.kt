package com.lyrismet.incadent.feature.combat.domain.preparation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AverageLevelTest {
    @Test
    fun `no one in the fight has no average`() {
        assertNull(averageLevelTenths(emptyList()))
    }

    @Test
    fun `members without a level have no average`() {
        assertNull(averageLevelTenths(listOf(null, null)))
    }

    @Test
    fun `members without a level are left out`() {
        assertEquals(40, averageLevelTenths(listOf(3, null, 5)))
    }

    @Test
    fun `a fraction keeps one decimal`() {
        assertEquals(43, averageLevelTenths(listOf(4, 4, 5)))
    }

    @Test
    fun `the text uses the separator and drops a zero fraction`() {
        assertEquals("4,3", tenthsText(43, ','))
        assertEquals("4.3", tenthsText(43, '.'))
        assertEquals("4", tenthsText(40, ','))
    }
}
