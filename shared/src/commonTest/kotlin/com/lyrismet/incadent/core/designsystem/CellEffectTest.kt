package com.lyrismet.incadent.core.designsystem

import com.lyrismet.incadent.core.designsystem.component.CellEffect
import com.lyrismet.incadent.core.designsystem.component.cellVariation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `a cell always gets the same variation`() {
        val a = cellVariation(5, 2)
        val b = cellVariation(5, 2)
        assertEquals(a.phase, b.phase)
        assertEquals(a.turn, b.turn)
        assertEquals(a.clockwise, b.clockwise)
    }

    @Test
    fun `neighbouring cells do not move in step`() {
        val phases = (0..3).map { cellVariation(4, it).phase }.toSet()
        assertEquals(4, phases.size)
    }

    @Test
    fun `a phase stays inside one lap`() {
        (1..9).forEach { level -> (0..9).forEach { assertTrue(cellVariation(level, it).phase in 0f..1f) } }
    }
}
