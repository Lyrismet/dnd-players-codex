package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class NpcMergeFieldsTest {
    private fun npc(
        description: String = "",
        race: String = "",
        speciesId: Long? = null,
        locations: List<Long> = emptyList(),
        status: NpcStatus = NpcStatus.NEUTRAL,
    ) = Npc(1, "Вазгар", status, NpcLifeState.ALIVE, description, locations, race, speciesId = speciesId)

    @Test
    fun `an empty field is taken from the other record`() {
        val filled = npc().filledFrom(npc(description = "орк", race = "Орк", speciesId = 4))
        assertEquals("орк", filled.description)
        assertEquals("Орк", filled.race)
        assertEquals(4L, filled.speciesId)
    }

    @Test
    fun `a filled field is never overwritten`() {
        val filled =
            npc(
                description = "мой",
                status = NpcStatus.FRIEND,
            ).filledFrom(npc(description = "чужой", status = NpcStatus.ENEMY))
        assertEquals("мой", filled.description)
        assertEquals(NpcStatus.FRIEND, filled.status)
    }

    @Test
    fun `places are joined without repeats`() {
        assertEquals(
            listOf(1L, 2L, 3L),
            npc(locations = listOf(1, 2)).filledFrom(npc(locations = listOf(2, 3))).locationIds,
        )
    }

    @Test
    fun `vulnerabilities are taken only when the survivor has none`() {
        val other = npc().copy(vulnerabilities = mapOf(DamageType.FIRE to DamageState.VULNERABILITY))
        assertEquals(other.vulnerabilities, npc().filledFrom(other).vulnerabilities)
    }
}
