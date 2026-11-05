package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.ResourceTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotColumnTest {
    private fun slot(
        level: Int,
        max: Int,
        id: Long = level.toLong(),
    ) = CharacterResource(id = id, name = "", max = max, current = max, slotLevel = level)

    private fun feature(
        id: Long,
        position: Int,
    ) = CharacterResource(id = id, name = "f$id", max = 2, current = 2, position = position)

    @Test
    fun `columns are in level order and skip a level without slots`() {
        val columns = slotColumns(listOf(slot(3, 2), slot(1, 4), slot(2, 0), feature(9, 0)))
        assertEquals(listOf(1, 3), columns.map { it.level })
    }

    @Test
    fun `no slots give no columns`() {
        assertTrue(slotColumns(listOf(feature(1, 0))).isEmpty())
    }

    @Test
    fun `features leave the slots out and keep the player order`() {
        val list = featureResources(listOf(feature(2, 1), slot(1, 2), feature(1, 0)))
        assertEquals(listOf(1L, 2L), list.map { it.id })
    }

    @Test
    fun `maxima list every level with zero for the missing ones`() {
        val maxima = slotMaxima(listOf(slot(1, 4), slot(3, 2)))
        assertEquals(9, maxima.size)
        assertEquals(4, maxima[1])
        assertEquals(0, maxima[2])
        assertEquals(2, maxima[3])
    }

    @Test
    fun `the next position goes after the last one`() {
        assertEquals(0, nextPosition(emptyList()))
        assertEquals(3, nextPosition(listOf(feature(1, 0), feature(2, 2))))
    }

    @Test
    fun `slot templates are hidden once there are slots`() {
        assertTrue(ResourceTemplate.SPELL_SLOTS in availableTemplates(listOf(feature(1, 0))))
        assertFalse(ResourceTemplate.SPELL_SLOTS in availableTemplates(listOf(slot(1, 2))))
        assertTrue(ResourceTemplate.RAGE in availableTemplates(listOf(slot(1, 2))))
    }

    @Test
    fun `the maximum stays between zero and ninety nine`() {
        assertEquals(0, 0.steppedMax(-1))
        assertEquals(99, 99.steppedMax(1))
        assertEquals(5, 4.steppedMax(1))
    }

    @Test
    fun `a lit cell is spent and a dark one comes back`() {
        assertEquals(2, cellPressed(current = 3, index = 0))
        assertEquals(2, cellPressed(current = 3, index = 2))
        assertEquals(3, cellPressed(current = 2, index = 2))
        assertEquals(1, cellPressed(current = 0, index = 3))
    }
}
