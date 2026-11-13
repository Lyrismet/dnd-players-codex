package com.lyrismet.incadent.core.designsystem

import com.lyrismet.incadent.core.designsystem.component.CellEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class CellEffectTest {
    @Test
    fun `the first three levels stand still`() {
        (1..3).forEach { assertEquals(CellEffect.None, CellEffect.forLevel(it)) }
    }

    @Test
    fun `levels four to six give sparks`() {
        (4..6).forEach { assertEquals(CellEffect.Sparks, CellEffect.forLevel(it)) }
    }

    @Test
    fun `levels seven and eight add the pulse`() {
        (7..8).forEach { assertEquals(CellEffect.SparksPulse, CellEffect.forLevel(it)) }
    }

    @Test
    fun `level nine orbits`() {
        assertEquals(CellEffect.Orbit, CellEffect.forLevel(9))
    }
}
