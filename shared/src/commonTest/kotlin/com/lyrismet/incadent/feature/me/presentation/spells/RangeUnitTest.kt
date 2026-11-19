package com.lyrismet.incadent.feature.me.presentation.spells

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RangeUnitTest {
    private val suffix = { unit: RangeUnit -> if (unit == RangeUnit.FEET) "фт" else "м" }

    @Test
    fun `the unit is read from a number with its unit`() {
        assertEquals(RangeUnit.FEET, rangeUnitOf("60 фт"))
        assertEquals(RangeUnit.METERS, rangeUnitOf("18 м"))
        assertEquals(RangeUnit.FEET, rangeUnitOf("30 ft"))
        assertEquals(RangeUnit.METERS, rangeUnitOf("9m"))
    }

    @Test
    fun `words and an empty text have no unit`() {
        assertNull(rangeUnitOf("Касание"))
        assertNull(rangeUnitOf(""))
        assertNull(rangeUnitOf("на себя"))
    }

    @Test
    fun `the presets of both units match`() {
        assertEquals(listOf(30, 60), RangeUnit.FEET.presets)
        assertEquals(listOf(9, 18), RangeUnit.METERS.presets)
    }

    @Test
    fun `feet turn into meters and back`() {
        assertEquals("9 м", convertRange("30 фт", RangeUnit.METERS, suffix))
        assertEquals("18 м", convertRange("60 фт", RangeUnit.METERS, suffix))
        assertEquals("27 м", convertRange("90 фт", RangeUnit.METERS, suffix))
        assertEquals("30 фт", convertRange("9 м", RangeUnit.FEET, suffix))
        assertEquals("60 фт", convertRange("18 м", RangeUnit.FEET, suffix))
    }

    @Test
    fun `a range in the unit already or in words stays`() {
        assertEquals("30 фт", convertRange("30 фт", RangeUnit.FEET, suffix))
        assertEquals("Касание", convertRange("Касание", RangeUnit.METERS, suffix))
    }
}
