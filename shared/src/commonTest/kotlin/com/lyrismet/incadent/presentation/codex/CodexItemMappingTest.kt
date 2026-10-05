package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val lifeLabels = mapOf(NpcLifeState.ALIVE to "Жив", NpcLifeState.DEAD to "Мёртв")

private val partyCardLabels =
    PartyCardLabels(
        presence = mapOf(PartyPresence.IN to "В составе", PartyPresence.AWAY to "Отсутствует"),
        you = "Ваш персонаж",
        playerPrefix = "Игрок:",
        armorClassShort = "КД",
        hpShort = "HP",
    )

private val lira =
    PartyMember(
        id = 7,
        name = "Лира",
        characterClass = "Волшебница",
        race = "Эльфийка",
        level = 5,
        playerName = "",
        isPlayerCharacter = true,
        presence = PartyPresence.IN,
        hpMax = 28,
        hpCurrent = 22,
        armorClass = 12,
        initiativeBonus = 3,
        description = "",
        portraitUri = null,
    )

private val bran =
    lira.copy(
        id = 8,
        name = "Бран",
        characterClass = "Плут",
        isPlayerCharacter = false,
        playerName = "Даня",
        presence = PartyPresence.AWAY,
        hpMax = 31,
        hpCurrent = 31,
        armorClass = 15,
    )

private val ragnar =
    Npc(
        id = 1,
        name = "Рагнар",
        status = NpcStatus.FRIEND,
        lifeState = NpcLifeState.ALIVE,
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
    fun `Npc toCodexItem subtitle combines race and faction`() {
        val item = ragnar.toCodexItem(statusLabels = mapOf(NpcStatus.FRIEND to "Друг"), lifeLabels = lifeLabels)
        assertEquals("Дварф · Гильдия", item.subtitle)
    }

    @Test
    fun `Location toCodexItem subtitle combines type and region`() {
        val forge = Location(id = 1, name = "Кузница", type = "Мастерская", description = "", region = "Север")
        assertEquals("Мастерская · Север", forge.toCodexItem().subtitle)
    }

    @Test
    fun `Npc toCodexItem subtitle has no dangling separator when race and faction are blank`() {
        val bare = ragnar.copy(race = "", faction = "")
        val item = bare.toCodexItem(statusLabels = mapOf(NpcStatus.FRIEND to "Друг"), lifeLabels = lifeLabels)
        assertEquals("", item.subtitle)
    }

    @Test
    fun `Location toCodexItem subtitle has no dangling separator when region is blank`() {
        val forge = Location(id = 1, name = "Кузница", type = "Мастерская", description = "", region = "")
        assertEquals("Мастерская", forge.toCodexItem().subtitle)
    }

    @Test
    fun `Npc toCodexItem adds a life badge only for the dead`() {
        val dead = ragnar.copy(id = 2, lifeState = NpcLifeState.DEAD)
        val statusLabels = mapOf(NpcStatus.FRIEND to "Друг")

        assertNull(ragnar.toCodexItem(statusLabels, lifeLabels).lifeBadge)
        assertEquals("Мёртв", dead.toCodexItem(statusLabels, lifeLabels).lifeBadge?.label)
        assertTrue(dead.toCodexItem(statusLabels, lifeLabels).isDead)
    }

    @Test
    fun `PartyMember matchesQuery matches class and player name`() {
        assertTrue(bran.matchesQuery("плут"))
        assertTrue(bran.matchesQuery("даня"))
        assertFalse(bran.matchesQuery("гоблин"))
    }

    @Test
    fun `PartyMember toCodexItems shows the you flag for the player character`() {
        val item = listOf(lira).toPartyCodexItems(partyCardLabels).single()
        assertEquals("Ваш персонаж", item.subtitle)
        assertEquals("КД 12 · HP 22/28", item.meta)
    }

    @Test
    fun `PartyMember toCodexItems shows the player's name for everyone else`() {
        val item = listOf(bran).toPartyCodexItems(partyCardLabels).single()
        assertEquals("Игрок: Даня", item.subtitle)
        assertEquals("Отсутствует", item.presenceLabel)
    }

    @Test
    fun `PartyMember toCodexItems leaves the subtitle blank when no player is set`() {
        val unclaimed = bran.copy(playerName = "")
        assertEquals("", listOf(unclaimed).toPartyCodexItems(partyCardLabels).single().subtitle)
    }
}
