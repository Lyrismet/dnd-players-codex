package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val ragnar =
    Npc(
        id = 1,
        name = "Рагнар",
        status = NpcStatus.FRIEND,
        description = "Кузнец из Кузницы",
        locationId = null,
        race = "Дварф",
        faction = "Гильдия",
    )

class CodexItemMappingTest {
    @Test
    fun `Npc matchesQuery matches everything for an empty query`() {
        assertTrue(ragnar.matchesQuery(""))
    }

    @Test
    fun `Npc matchesQuery matches the name case-insensitively`() {
        assertTrue(ragnar.matchesQuery("рагнар"))
        assertTrue(ragnar.matchesQuery("РАГНАР"))
    }

    @Test
    fun `Npc matchesQuery matches the description`() {
        assertTrue(ragnar.matchesQuery("кузнец"))
    }

    @Test
    fun `Npc matchesQuery rejects unrelated text`() {
        assertFalse(ragnar.matchesQuery("гоблин"))
    }

    @Test
    fun `Quest matchesQuery matches everything for an empty query`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = null,
                locationId = null,
            )
        assertTrue(quest.matchesQuery("", emptyMap()))
    }

    @Test
    fun `Quest matchesQuery matches title and reward`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = null,
                locationId = null,
            )
        assertTrue(quest.matchesQuery("меч", emptyMap()))
        assertTrue(quest.matchesQuery("золота", emptyMap()))
    }

    @Test
    fun `Quest matchesQuery matches the description`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = null,
                locationId = null,
                description = "Найти три фрагмента клинка",
            )
        assertTrue(quest.matchesQuery("фрагмент", emptyMap()))
        assertFalse(quest.matchesQuery("дракон", emptyMap()))
    }

    @Test
    fun `Quest matchesQuery matches the giver name`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = 1,
                locationId = null,
            )
        assertTrue(quest.matchesQuery("рагнар", mapOf(1L to ragnar)))
    }

    @Test
    fun `Quest matchesQuery is safe when the giver is unknown`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = 99,
                locationId = null,
            )
        assertFalse(quest.matchesQuery("рагнар", emptyMap()))
    }

    @Test
    fun `Location matchesQuery matches name and type`() {
        val forge = Location(id = 1, name = "Кузница", type = "Мастерская", description = "", region = "Север")
        assertTrue(forge.matchesQuery(""))
        assertTrue(forge.matchesQuery("кузница"))
        assertTrue(forge.matchesQuery("мастерская"))
        assertFalse(forge.matchesQuery("таверна"))
    }

    @Test
    fun `Npc toCodexItems subtitle combines race and faction`() {
        val items = listOf(ragnar).toCodexItems(statusLabels = mapOf(NpcStatus.FRIEND to "Друг"))
        assertEquals("Дварф · Гильдия", items.single().subtitle)
    }

    @Test
    fun `Location toCodexItems subtitle combines type and region`() {
        val forge = Location(id = 1, name = "Кузница", type = "Мастерская", description = "", region = "Север")
        assertEquals("Мастерская · Север", listOf(forge).toCodexItems().single().subtitle)
    }

    @Test
    fun `Npc toCodexItems subtitle has no dangling separator when race and faction are blank`() {
        val bare = ragnar.copy(race = "", faction = "")
        val items = listOf(bare).toCodexItems(statusLabels = mapOf(NpcStatus.FRIEND to "Друг"))
        assertEquals("", items.single().subtitle)
    }

    @Test
    fun `Location toCodexItems subtitle has no dangling separator when region is blank`() {
        val forge = Location(id = 1, name = "Кузница", type = "Мастерская", description = "", region = "")
        assertEquals("Мастерская", listOf(forge).toCodexItems().single().subtitle)
    }
}
