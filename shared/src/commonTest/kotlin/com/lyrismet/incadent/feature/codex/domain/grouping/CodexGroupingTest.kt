package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
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
            topLevel = "Верхний уровень",
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

    // north holds castle (which holds tavern) and hall; south holds mill and barn
    private val north = location(1, "Север")
    private val castle = location(2, "Замок", parentId = 1)
    private val tavern = location(3, "Таверна", parentId = 2)
    private val hall = location(6, "Холл", parentId = 1)
    private val south = location(4, "Юг")
    private val mill = location(5, "Мельница", parentId = 4)
    private val barn = location(7, "Амбар", parentId = 4)
    private val places = listOf(north, castle, tavern, hall, south, mill, barn)

    @Test
    fun `npcs group by the region of their place with the no-place group last`() {
        val npcs =
            listOf(
                npc(1, "Анна", NpcStatus.FRIEND, locationIds = listOf(castle.id), faction = "Гильдия"),
                npc(2, "Борис", NpcStatus.ENEMY, locationIds = listOf(mill.id), race = "Человек"),
                npc(3, "Вера", NpcStatus.NEUTRAL),
                npc(4, "Глеб", NpcStatus.FRIEND, NpcLifeState.DEAD, locationIds = listOf(castle.id)),
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
                npc(1, "Анна", locationIds = listOf(castle.id), faction = "Гильдия", race = "Эльф"),
                npc(2, "Борис", locationIds = listOf(mill.id), race = "Человек"),
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
        val giver = npc(10, "Гром", locationIds = listOf(mill.id))
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
    fun `npcs of one shared world group by country and not by the world`() {
        val world = location(1, "Материальный план")
        val sanChitlan = location(2, "Сан-Читлан", parentId = 1)
        val tkhalopl = location(3, "Ткхалопль", parentId = 1)
        val village = location(4, "Деревня Ибиотса", parentId = 2)
        val all = listOf(world, sanChitlan, tkhalopl, village)
        val npcs =
            listOf(
                npc(1, "Анна", locationIds = listOf(village.id)),
                npc(2, "Борис", locationIds = listOf(tkhalopl.id)),
                npc(3, "Вера", locationIds = listOf(sanChitlan.id)),
            )

        val groups = groupNpcs(npcs, all, NpcGroupBy.REGION, labels)

        assertEquals(listOf("САН-ЧИТЛАН", "ТКХАЛОПЛЬ"), groups.map { it.title })
        assertEquals(listOf("Анна", "Вера"), groups[0].items.map { it.entity.name })
    }

    @Test
    fun `an npc met in two regions shows up under both`() {
        val roamer = npc(1, "Анна", locationIds = listOf(castle.id, mill.id))

        val groups = groupNpcs(listOf(roamer), places, NpcGroupBy.REGION, labels)

        assertEquals(listOf("СЕВЕР", "ЮГ"), groups.map { it.title })
        assertEquals("▲ Замок, Мельница", groups[0].items.single().subtitle)
    }

    @Test
    fun `places list the top level ones as plain rows and give every place that holds places its own section`() {
        val groups = groupLocations(places, LocationGroupBy.REGION, places, labels)

        assertEquals(listOf(null, "СЕВЕР", "ЗАМОК", "ЮГ"), groups.map { it.title })
        assertEquals(listOf("Север", "Юг"), groups[0].items.map { it.entity.name })
        // a section holds only what lies directly in it, by name
        assertEquals(listOf("Замок", "Холл"), groups[1].items.map { it.entity.name })
        assertEquals(listOf("Таверна"), groups[2].items.map { it.entity.name })
        assertEquals(listOf("Амбар", "Мельница"), groups[3].items.map { it.entity.name })
        assertEquals(listOf(2, 1, 2), groups.drop(1).map { it.count })
    }

    @Test
    fun `a section header carries the path above its place cut to the last two places`() {
        val deep = location(10, "Склеп", parentId = 3)
        val crypt = location(11, "Ниша", parentId = 10)
        val all = places + deep + crypt

        val groups = groupLocations(all, LocationGroupBy.REGION, all, labels)

        assertEquals(null, groups.first { it.title == "СЕВЕР" }.path)
        assertEquals("Север", groups.first { it.title == "ЗАМОК" }.path)
        assertEquals("… › Замок › Таверна", groups.first { it.title == "СКЛЕП" }.path)
    }

    @Test
    fun `place rows in the tree carry no subtitle`() {
        val items = groupLocations(places, LocationGroupBy.REGION, places, labels).flatMap { it.items }

        assertTrue(items.all { it.subtitle == null })
    }

    @Test
    fun `a folded section keeps its count and shows no rows`() {
        val groups = groupLocations(places, LocationGroupBy.REGION, places, labels, collapsedIds = setOf(north.id))

        val section = groups.first { it.title == "СЕВЕР" }
        assertTrue(section.collapsed)
        assertTrue(section.items.isEmpty())
        assertEquals(2, section.count)
        assertEquals(north.id, section.toggleId)
    }

    @Test
    fun `a search that hides a parent still shows the section of its matching child`() {
        val shown = listOf(tavern)

        val groups = groupLocations(shown, LocationGroupBy.REGION, places, labels)

        assertEquals(listOf("ЗАМОК"), groups.map { it.title })
        assertEquals(listOf("Таверна"), groups.single().items.map { it.entity.name })
    }

    @Test
    fun `places on a parent loop are still listed`() {
        val a = location(20, "А", parentId = 21)
        val b = location(21, "Б", parentId = 20)
        val loop = listOf(a, b)

        val groups = groupLocations(loop, LocationGroupBy.REGION, loop, labels)

        assertEquals(setOf("А", "Б"), groups.flatMap { it.items }.map { it.entity.name }.toSet())
    }

    @Test
    fun `places as a list form one untitled group sorted by name`() {
        val groups = groupLocations(places, LocationGroupBy.NONE, places, labels)

        assertEquals(listOf(null), groups.map { it.title })
        val names = listOf("Амбар", "Замок", "Мельница", "Север", "Таверна", "Холл", "Юг")
        assertEquals(names, groups.single().items.map { it.entity.name })
    }

    @Test
    fun `places as a list show their whole path and mark a top level place`() {
        val items = groupLocations(places, LocationGroupBy.NONE, places, labels).single().items

        assertEquals("Север › Замок", items.first { it.entity.name == "Таверна" }.subtitle)
        assertEquals("Верхний уровень", items.first { it.entity.name == "Север" }.subtitle)
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
