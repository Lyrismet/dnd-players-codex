package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NpcDuplicateTest {
    private fun npc(
        id: Long,
        name: String,
    ) = Npc(id, name, NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")

    private val known = listOf(npc(1, "Вазгар"), npc(2, "Кира"))

    @Test
    fun `a taken name finds its record whatever the letter case and spaces`() {
        assertEquals(1L, duplicateNpcOf("  вазгар ", known, exceptId = null)?.id)
    }

    @Test
    fun `the record being edited is not its own twin`() {
        assertNull(duplicateNpcOf("Вазгар", known, exceptId = 1))
    }

    @Test
    fun `a free or empty name has no twin`() {
        assertNull(duplicateNpcOf("Ренгар", known, exceptId = null))
        assertNull(duplicateNpcOf("   ", known, exceptId = null))
    }
}
