package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class NpcNamingTest {
    private fun npc(
        id: Long,
        name: String,
        speciesId: Long?,
    ) = Npc(id, name, NpcStatus.ENEMY, NpcLifeState.ALIVE, "", emptyList(), "", speciesId = speciesId)

    @Test
    fun `an empty name offers nothing`() {
        assertEquals(emptyList(), nameChoices("  ", null, emptyList()))
    }

    @Test
    fun `a free name offers a new npc and a caption`() {
        assertEquals(
            listOf(NameChoice.New("Вазгар"), NameChoice.Plain("Вазгар")),
            nameChoices(" Вазгар ", null, listOf(npc(1, "Мирра", null))),
        )
    }

    @Test
    fun `a taken name offers the known npc and no new one`() {
        assertEquals(
            listOf(NameChoice.Existing(1, "Вазгар"), NameChoice.Plain("вазгар")),
            nameChoices("вазгар", null, listOf(npc(1, "Вазгар", null))),
        )
    }

    @Test
    fun `known npcs of the same species come first and only three are shown`() {
        val npcs =
            listOf(npc(1, "Орк А", 5), npc(2, "Орк Б", 7), npc(3, "Орк В", 7), npc(4, "Орк Г", 7), npc(5, "Орк Д", 7))
        val ids = nameChoices("Орк", 7, npcs).filterIsInstance<NameChoice.Existing>().map { it.npcId }
        assertEquals(listOf(2L, 3L, 4L), ids)
    }
}
