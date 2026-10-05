package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodexGroupingTest {
    private val labels =
        CodexGroupLabels(
            noRegionForQuest = "Без региона",
            noPlace = "Место не указано",
            noFaction = "Без фракции",
            noGiver = "Неизвестно от кого",
            giverPrefix = "От:",
            noType = "Без типа",
            npcStatus =
                mapOf(
                    NpcStatus.FRIEND to "Друг",
                    NpcStatus.ENEMY to "Враг",
                    NpcStatus.NEUTRAL to "Нейтрал",
                ),
            questStatus =
                mapOf(
                    QuestStatus.ACTIVE to "Активен",
                    QuestStatus.COMPLETED to "Выполнен",
                    QuestStatus.FAILED to "Провален",
                ),
        )

    // north (no region) holds castle, which holds tavern; south (no region) holds mill
    private val north = location(1, "Север", region = "", type = "Регион")
    private val castle = location(2, "Замок", region = "Север", type = "Крепость")
    private val tavern = location(3, "Таверна", region = "Замок", type = "Таверна")
    private val south = location(4, "Юг", region = "", type = "Регион")
    private val mill = location(5, "Мельница", region = "Юг")
    private val places = listOf(north, castle, tavern, south, mill)

    @Test
    fun `npcs group by the region of their place with the no-place group last`() {
        val npcs =
            listOf(
                npc(1, "Анна", NpcStatus.FRIEND, locationId = castle.id, faction = "Гильдия"),
                npc(2, "Борис", NpcStatus.ENEMY, locationId = mill.id, race = "Человек"),
                npc(3, "Вера", NpcStatus.NEUTRAL),
                npc(4, "Глеб", NpcStatus.FRIEND, NpcLifeState.DEAD, locationId = castle.id),
            )

        val groups = groupNpcs(npcs, places, NpcGroupBy.REGION, labels)

        assertEquals(listOf("СЕВЕР", "ЮГ", "МЕСТО НЕ УКАЗАНО"), groups.map { it.title })
        assertEquals(listOf("Анна", "Глеб"), groups[0].items.map { it.entity.name })
        assertEquals(listOf("Борис"), groups[1].items.map { it.entity.name })
        assertEquals(listOf("Вера"), groups[2].items.map { it.entity.name })
    }

    @Test
    fun `a region npc subtitle names the place and the faction or race`() {
        val npcs =
            listOf(
                npc(1, "Анна", locationId = castle.id, faction = "Гильдия", race = "Эльф"),
                npc(2, "Борис", locationId = mill.id, race = "Человек"),
                npc(3, "Вера"),
            )

        val items = groupNpcs(npcs, places, NpcGroupBy.REGION, labels).flatMap { it.items }

        assertEquals("▲ Замок · Гильдия", items.first { it.entity.name == "Анна" }.subtitle)
        assertEquals("▲ Мельница · Человек", items.first { it.entity.name == "Борис" }.subtitle)
        assertNull(items.first { it.entity.name == "Вера" }.subtitle)
    }

    @Test
    fun `npcs group by relation in the status order with no default subtitle`() {
        val npcs =
            listOf(
                npc(1, "Вера", NpcStatus.NEUTRAL),
                npc(2, "Борис", NpcStatus.ENEMY),
                npc(3, "Глеб", NpcStatus.FRIEND, NpcLifeState.DEAD),
                npc(4, "Анна", NpcStatus.FRIEND),
            )

        val groups = groupNpcs(npcs, places, NpcGroupBy.RELATION, labels)

        assertEquals(listOf("ДРУГ", "ВРАГ", "НЕЙТРАЛ"), groups.map { it.title })
        assertEquals(listOf("Анна", "Глеб"), groups[0].items.map { it.entity.name })
        assertTrue(groups.flatMap { it.items }.all { it.subtitle == null })
    }

    @Test
    fun `npcs group by faction with the no-faction group last`() {
        val npcs =
            listOf(
                npc(1, "Анна", faction = "Гильдия"),
                npc(2, "Борис", faction = "Орден"),
                npc(3, "Вера", faction = "—"),
                npc(4, "Глеб", faction = "Гильдия"),
            )

        val groups = groupNpcs(npcs, places, NpcGroupBy.FACTION, labels)

        assertEquals(listOf("ГИЛЬДИЯ", "ОРДЕН", "БЕЗ ФРАКЦИИ"), groups.map { it.title })
        assertEquals(listOf("Анна", "Глеб"), groups[0].items.map { it.entity.name })
    }

    @Test
    fun `npcs as a list form one untitled group with the dead last`() {
        val npcs =
            listOf(
                npc(1, "Глеб", lifeState = NpcLifeState.DEAD),
                npc(2, "Борис"),
                npc(3, "Анна"),
            )

        val groups = groupNpcs(npcs, places, NpcGroupBy.NONE, labels)

        assertEquals(listOf(null), groups.map { it.title })
        assertEquals(listOf("Анна", "Борис", "Глеб"), groups.single().items.map { it.entity.name })
    }

    @Test
    fun `an empty npc list produces no groups for any grouping`() {
        NpcGroupBy.entries.forEach { by ->
            assertTrue(groupNpcs(emptyList(), places, by, labels).isEmpty())
        }
    }

    @Test
    fun `quests group by the region of their where place with the no-region group last`() {
        val giver = npc(10, "Гром", locationId = mill.id)
        val quests =
            listOf(
                quest(1, "Ключ", QuestStatus.ACTIVE, locationId = castle.id),
                quest(2, "Гнездо", QuestStatus.COMPLETED, givenByNpcId = giver.id),
                quest(3, "Пир", QuestStatus.FAILED),
                quest(4, "Амулет", QuestStatus.ACTIVE, locationId = castle.id),
            )

        val groups = groupQuests(quests, listOf(giver), places, QuestGroupBy.REGION, labels)

        assertEquals(listOf("СЕВЕР", "ЮГ", "БЕЗ РЕГИОНА"), groups.map { it.title })
        assertEquals(listOf("Амулет", "Ключ"), groups[0].items.map { it.title })
        assertEquals(listOf("Гнездо"), groups[1].items.map { it.title })
        assertEquals(listOf("Пир"), groups[2].items.map { it.title })
    }

    @Test
    fun `quests group by status in the status order and drop empty statuses`() {
        val quests =
            listOf(
                quest(1, "Ключ", QuestStatus.ACTIVE),
                quest(2, "Гнездо", QuestStatus.COMPLETED),
                quest(3, "Амулет", QuestStatus.ACTIVE),
            )

        val groups = groupQuests(quests, emptyList(), places, QuestGroupBy.STATUS, labels)

        assertEquals(listOf("АКТИВЕН", "ВЫПОЛНЕН"), groups.map { it.title })
        assertEquals(listOf("Амулет", "Ключ"), groups[0].items.map { it.title })
    }

    @Test
    fun `quests group by their giver with the unknown-giver group last`() {
        val grom = npc(10, "Гром")
        val quests =
            listOf(
                quest(1, "Ключ", QuestStatus.ACTIVE),
                quest(2, "Гнездо", QuestStatus.COMPLETED, givenByNpcId = grom.id),
                quest(3, "Пир", QuestStatus.FAILED),
                quest(4, "Амулет", QuestStatus.ACTIVE),
            )

        val groups = groupQuests(quests, listOf(grom), places, QuestGroupBy.GIVER, labels)

        assertEquals(listOf("ОТ: ГРОМ", "НЕИЗВЕСТНО ОТ КОГО"), groups.map { it.title })
        assertEquals(listOf("Гнездо"), groups[0].items.map { it.title })
        assertEquals(listOf("Амулет", "Ключ", "Пир"), groups[1].items.map { it.title })
    }

    @Test
    fun `quests as a list sort by status then name`() {
        val quests =
            listOf(
                quest(1, "Пир", QuestStatus.FAILED),
                quest(2, "Гнездо", QuestStatus.COMPLETED),
                quest(3, "Ключ", QuestStatus.ACTIVE),
                quest(4, "Амулет", QuestStatus.ACTIVE),
            )

        val groups = groupQuests(quests, emptyList(), places, QuestGroupBy.NONE, labels)

        assertEquals(listOf(null), groups.map { it.title })
        val titles = listOf("Амулет", "Ключ", "Гнездо", "Пир")
        assertEquals(titles, groups.single().items.map { it.title })
    }

    @Test
    fun `an empty quest list produces no groups for any grouping`() {
        QuestGroupBy.entries.forEach { by ->
            assertTrue(groupQuests(emptyList(), emptyList(), places, by, labels).isEmpty())
        }
    }

    @Test
    fun `places group by their resolved region and name the own region only when it differs`() {
        val groups = groupLocations(places, LocationGroupBy.REGION, places, labels)

        assertEquals(listOf("СЕВЕР", "ЮГ", "МЕСТО НЕ УКАЗАНО"), groups.map { it.title })
        assertEquals(listOf("Замок", "Таверна"), groups[0].items.map { it.entity.name })
        assertEquals("Крепость", groups[0].items[0].subtitle)
        assertEquals("Таверна · в Замок", groups[0].items[1].subtitle)
        assertEquals("Город", groups[1].items.single().subtitle)
        assertEquals(listOf("Север", "Юг"), groups[2].items.map { it.entity.name })
    }

    @Test
    fun `places group by type with the no-type group last and no default subtitle`() {
        val cave = location(6, "Пещера", region = "", type = "")
        val all = places + cave

        val groups = groupLocations(all, LocationGroupBy.TYPE, all, labels)

        val titles = listOf("ГОРОД", "КРЕПОСТЬ", "РЕГИОН", "ТАВЕРНА", "БЕЗ ТИПА")
        assertEquals(titles, groups.map { it.title })
        assertEquals(listOf("Север", "Юг"), groups[2].items.map { it.entity.name })
        assertEquals(listOf("Пещера"), groups.last().items.map { it.entity.name })
        assertTrue(groups.flatMap { it.items }.all { it.subtitle == null })
    }

    @Test
    fun `places as a list form one untitled group sorted by name`() {
        val groups = groupLocations(places, LocationGroupBy.NONE, places, labels)

        assertEquals(listOf(null), groups.map { it.title })
        val names = listOf("Замок", "Мельница", "Север", "Таверна", "Юг")
        assertEquals(names, groups.single().items.map { it.entity.name })
    }

    @Test
    fun `an empty place list produces no groups for any grouping`() {
        LocationGroupBy.entries.forEach { by ->
            assertTrue(groupLocations(emptyList(), by, emptyList(), labels).isEmpty())
        }
    }

    @Test
    fun `a grouping selection changes only the tab it was chosen for`() {
        val selection = CodexGroupingSelection().select(CodexGroupBy.Npc(NpcGroupBy.RELATION))

        assertEquals(NpcGroupBy.RELATION, selection.npc)
        assertEquals(QuestGroupBy.REGION, selection.quest)
        assertEquals(LocationGroupBy.REGION, selection.place)
    }
}
