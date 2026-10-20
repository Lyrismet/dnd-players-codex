package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.domain.grouping.CodexRegions
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val lifeLabels = mapOf(NpcLifeState.ALIVE to "Жив", NpcLifeState.DEAD to "Мёртв")

private val noRegions = CodexRegions(emptyList(), emptyList())

private val partyCardLabels =
    PartyCardLabels(
        presence = mapOf(PartyPresence.IN to "В составе", PartyPresence.AWAY to "Отсутствует"),
        you = "Ваш персонаж",
        playerPrefix = "Игрок:",
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
        description = "",
        portrait = null,
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
    )

private val ragnar =
    Npc(
        id = 1,
        name = "Рагнар",
        status = NpcStatus.FRIEND,
        lifeState = NpcLifeState.ALIVE,
        description = "Кузнец из Кузницы",
        locationIds = emptyList(),
        race = "Дварф",
    )

class CodexItemMappingTest {
    @Test
    fun `Npc matchesQuery matches everything for an empty query`() {
        assertTrue(ragnar.matchesQuery("", noRegions))
    }

    @Test
    fun `Npc matchesQuery matches the name case-insensitively`() {
        assertTrue(ragnar.matchesQuery("рагнар", noRegions))
        assertTrue(ragnar.matchesQuery("РАГНАР", noRegions))
    }

    @Test
    fun `Npc matchesQuery does not look into the description`() {
        assertFalse(ragnar.matchesQuery("кузнец", noRegions))
    }

    @Test
    fun `Npc matchesQuery rejects unrelated text`() {
        assertFalse(ragnar.matchesQuery("гоблин", noRegions))
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
        assertTrue(quest.matchesQuery("", noRegions))
    }

    @Test
    fun `Quest matchesQuery matches the title but not the reward`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = null,
                locationId = null,
            )
        assertTrue(quest.matchesQuery("меч", noRegions))
        assertFalse(quest.matchesQuery("золота", noRegions))
    }

    @Test
    fun `Quest matchesQuery does not look into the description`() {
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
        assertFalse(quest.matchesQuery("фрагмент", noRegions))
        assertFalse(quest.matchesQuery("дракон", noRegions))
    }

    @Test
    fun `Quest matchesQuery does not match the giver name`() {
        val quest =
            Quest(
                id = 1,
                title = "Меч",
                status = QuestStatus.ACTIVE,
                reward = "50 золота",
                givenByNpcId = 1,
                locationId = null,
            )
        assertFalse(quest.matchesQuery("рагнар", noRegions))
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
        assertFalse(quest.matchesQuery("рагнар", noRegions))
    }

    @Test
    fun `Location matchesQuery matches its name and the places above it`() {
        val town = Location(id = 1, name = "Мирдейл", description = "")
        val forge = Location(id = 2, name = "Кузница", description = "", parentId = 1)
        val regions = CodexRegions(emptyList(), listOf(town, forge))

        assertTrue(forge.matchesQuery("", regions))
        assertTrue(forge.matchesQuery("кузница", regions))
        assertTrue(forge.matchesQuery("мирдейл", regions))
        assertFalse(forge.matchesQuery("таверна", regions))
    }

    @Test
    fun `Npc toCodexItem subtitle is the race`() {
        val item = ragnar.toCodexItem(statusLabels = mapOf(NpcStatus.FRIEND to "Друг"), lifeLabels = lifeLabels)
        assertEquals("Дварф", item.subtitle)
    }

    @Test
    fun `Location toCodexItem carries the path it is given`() {
        val forge = Location(id = 2, name = "Кузница", description = "", parentId = 1)

        assertEquals("Мирдейл", forge.toCodexItem(subtitle = "Мирдейл").subtitle)
        assertNull(forge.toCodexItem().subtitle)
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
    fun `PartyMember matchesQuery matches the name but not class or player name`() {
        assertTrue(bran.matchesQuery("бран"))
        assertFalse(bran.matchesQuery("плут"))
        assertFalse(bran.matchesQuery("даня"))
        assertFalse(bran.matchesQuery("гоблин"))
    }

    @Test
    fun `PartyMember toCodexItems shows the you flag for the player character`() {
        val item = listOf(lira).toPartyCodexItems(partyCardLabels).single()
        assertEquals("Ваш персонаж", item.subtitle)
        assertEquals("HP 22/28", item.meta)
    }

    @Test
    fun `PartyMember toCodexItems shows a dash for hit points that are not set`() {
        val blank = lira.copy(hpMax = null, hpCurrent = null)

        assertEquals("HP —", listOf(blank).toPartyCodexItems(partyCardLabels).single().meta)
    }

    @Test
    fun `PartyMember toCodexItems shows the max for a current hp that is not set`() {
        val fresh = lira.copy(hpCurrent = null)

        assertEquals("HP 28/28", listOf(fresh).toPartyCodexItems(partyCardLabels).single().meta)
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
