package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CodexOrderingTest {
    @Test
    fun `dead npcs sort after the living ones`() {
        val sorted =
            sortNpcs(
                listOf(
                    npc(1, "Глеб", lifeState = NpcLifeState.DEAD),
                    npc(2, "Борис"),
                    npc(3, "Анна", lifeState = NpcLifeState.DEAD),
                    npc(4, "Вера"),
                ),
            )

        assertEquals(listOf("Борис", "Вера", "Анна", "Глеб"), sorted.map { it.name })
    }

    @Test
    fun `npcs sort by name regardless of case`() {
        val sorted = sortNpcs(listOf(npc(1, "борис"), npc(2, "Аня")))

        assertEquals(listOf("Аня", "борис"), sorted.map { it.name })
    }

    @Test
    fun `russian names sort in alphabetical order with yo placed with ye`() {
        val sorted = sortNpcs(listOf(npc(1, "Яна"), npc(2, "Жук"), npc(3, "Ёлка"), npc(4, "Борис")))

        assertEquals(listOf("Борис", "Ёлка", "Жук", "Яна"), sorted.map { it.name })
    }

    @Test
    fun `quests sort active then completed then failed then by name`() {
        val sorted =
            sortQuests(
                listOf(
                    quest(1, "Ястреб", QuestStatus.FAILED),
                    quest(2, "Ящик", QuestStatus.ACTIVE),
                    quest(3, "Башня", QuestStatus.COMPLETED),
                    quest(4, "Амулет", QuestStatus.ACTIVE),
                    quest(5, "Вор", QuestStatus.FAILED),
                ),
            )

        assertEquals(
            listOf("Амулет", "Ящик", "Башня", "Вор", "Ястреб"),
            sorted.map { it.title },
        )
    }

    @Test
    fun `locations sort by name`() {
        val places = listOf(location(1, "Таверна"), location(2, "Башня"), location(3, "Лес"))
        val sorted = sortLocations(places)

        assertEquals(listOf("Башня", "Лес", "Таверна"), sorted.map { it.name })
    }
}
