package com.lyrismet.dndcodex.presentation.codex

import com.lyrismet.dndcodex.domain.model.Location
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.Quest
import com.lyrismet.dndcodex.domain.model.QuestStatus
import kotlin.test.Test
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
}
