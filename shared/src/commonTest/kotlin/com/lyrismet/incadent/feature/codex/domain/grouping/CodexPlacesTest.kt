package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodexPlacesTest {
    // plane holds only valley (so it is skipped as a header), valley holds town, town holds temple;
    // wastes holds castle and tower
    private val plane = location(1, "Материальный план")
    private val valley = location(2, "Долина", parentId = 1)
    private val town = location(3, "Мирдейл", parentId = 2)
    private val temple = location(4, "Храм Рассвета", parentId = 3)
    private val wastes = location(5, "Пустоши")
    private val castle = location(6, "Замок", parentId = 5)
    private val tower = location(7, "Башня", parentId = 5)
    private val all = listOf(plane, valley, town, temple, wastes, castle, tower)
    private val tree = PlaceTree(all)

    @Test
    fun `a path runs from the top level place down to the place itself`() {
        assertEquals(
            listOf("Материальный план", "Долина", "Мирдейл", "Храм Рассвета"),
            tree.pathOf(temple).map { it.name },
        )
        assertEquals(listOf("Пустоши"), tree.pathOf(wastes).map { it.name })
    }

    @Test
    fun `ancestors label joins the places above with an arrow and is blank at the top`() {
        assertEquals("Материальный план › Долина › Мирдейл", tree.ancestorsLabel(temple))
        assertEquals("", tree.ancestorsLabel(plane))
    }

    @Test
    fun `a parent loop ends the path instead of spinning`() {
        val a = location(1, "А", parentId = 2)
        val b = location(2, "Б", parentId = 1)

        val path = PlaceTree(listOf(a, b)).pathOf(a)

        assertEquals(2, path.size)
    }

    @Test
    fun `a missing parent makes the place a top level one`() {
        val orphan = location(9, "Сирота", parentId = 404)

        assertEquals(listOf(orphan), PlaceTree(listOf(orphan)).pathOf(orphan))
    }

    @Test
    fun `with several top level places each of them is a region`() {
        assertEquals(wastes, tree.regionOf(castle))
        assertEquals(wastes, tree.regionOf(wastes))
        assertEquals(plane, tree.regionOf(temple))
        assertEquals(plane, tree.regionOf(valley))
    }

    @Test
    fun `a shared top level place is passed and its countries are the regions`() {
        val world = location(1, "Материальный план")
        val sanChitlan = location(2, "Сан-Читлан", parentId = 1)
        val tkhalopl = location(3, "Ткхалопль", parentId = 1)
        val village = location(4, "Деревня Ибиотса", parentId = 2)
        val hut = location(5, "Хижина", parentId = 4)
        val world3 = PlaceTree(listOf(world, sanChitlan, tkhalopl, village, hut))

        assertEquals(sanChitlan, world3.regionOf(village))
        assertEquals(sanChitlan, world3.regionOf(hut))
        assertEquals(sanChitlan, world3.regionOf(sanChitlan))
        assertEquals(tkhalopl, world3.regionOf(tkhalopl))
        // the shared place itself has no country, it stays its own region
        assertEquals(world, world3.regionOf(world))
    }

    @Test
    fun `places that hold a single place are passed down to where the tree first branches`() {
        val world = location(1, "Мир")
        val plane = location(2, "Материальный план", parentId = 1)
        val north = location(3, "Север", parentId = 2)
        val south = location(4, "Юг", parentId = 2)
        val town = location(5, "Город", parentId = 3)
        val chained = PlaceTree(listOf(world, plane, north, south, town))

        assertEquals(north, chained.regionOf(town))
        assertEquals(south, chained.regionOf(south))
        // wrappers above the branch are listed under the branching place
        assertEquals(plane, chained.regionOf(world))
        assertEquals(plane, chained.regionOf(plane))
    }

    @Test
    fun `a lone top level place is its own region`() {
        assertEquals(plane, PlaceTree(listOf(plane)).regionOf(plane))
    }

    @Test
    fun `the short path keeps the last two places above and cuts the start with an ellipsis`() {
        assertEquals("… › Долина › Мирдейл", tree.shortAncestorsLabel(temple))
        assertEquals("Материальный план › Долина", tree.shortAncestorsLabel(town))
        assertEquals("", tree.shortAncestorsLabel(plane))
    }

    @Test
    fun `roots are the places without a parent or whose parent is missing`() {
        val orphan = location(9, "Сирота", parentId = 404)
        val withOrphan = PlaceTree(all + orphan)

        assertEquals(listOf(1L, 5L, 9L), withOrphan.roots.map { it.id })
        assertEquals(listOf(6L, 7L), tree.childrenOf(wastes.id).map { it.id })
    }

    @Test
    fun `a place is inside itself and everything above it`() {
        assertTrue(tree.isInside(temple, town.id))
        assertTrue(tree.isInside(temple, plane.id))
        assertTrue(tree.isInside(temple, temple.id))
        assertFalse(tree.isInside(temple, wastes.id))
    }

    @Test
    fun `descendants cover every depth and leave the place itself out`() {
        assertEquals(setOf(2L, 3L, 4L), tree.descendantIds(plane.id))
        assertEquals(setOf(6L, 7L), tree.descendantIds(wastes.id))
        assertTrue(tree.descendantIds(temple.id).isEmpty())
    }

    @Test
    fun `an npc region list holds one region per distinct place and none without a place`() {
        val roamer = npc(1, locationIds = listOf(castle.id, tower.id, temple.id))
        val homeless = npc(2)
        val regions = CodexRegions(listOf(roamer, homeless), all)

        assertEquals(listOf(wastes, plane), regions.regionsOfNpc(roamer))
        assertTrue(regions.regionsOfNpc(homeless).isEmpty())
    }

    @Test
    fun `a quest takes the region of its place and falls back to the first place of its giver`() {
        val giver = npc(1, locationIds = listOf(temple.id))
        val placed = quest(1, locationId = castle.id)
        val viaGiver = quest(2, givenByNpcId = giver.id)
        val nowhere = quest(3)
        val regions = CodexRegions(listOf(giver), all)

        assertEquals(wastes, regions.regionOfQuest(placed))
        assertEquals(plane, regions.regionOfQuest(viaGiver))
        assertEquals(null, regions.regionOfQuest(nowhere))
    }

    @Test
    fun `an npc place search sees the names of every place above its places`() {
        val priest = npc(1, locationIds = listOf(temple.id))
        val regions = CodexRegions(listOf(priest), all)

        val names = regions.placeNamesOfNpc(priest)

        assertTrue("Мирдейл" in names)
        assertTrue("Храм Рассвета" in names)
        assertFalse("Пустоши" in names)
    }

    @Test
    fun `a place search sees only the places above it`() {
        val regions = CodexRegions(emptyList(), all)

        assertEquals(listOf("Материальный план", "Долина", "Мирдейл"), regions.placeNamesOfLocation(temple))
    }
}

internal fun location(
    id: Long,
    name: String,
    parentId: Long? = null,
) = Location(id = id, name = name, description = "", parentId = parentId)

internal fun npc(
    id: Long,
    name: String = "NPC $id",
    status: NpcStatus = NpcStatus.FRIEND,
    lifeState: NpcLifeState = NpcLifeState.ALIVE,
    locationIds: List<Long> = emptyList(),
    race: String = "",
    faction: String = "",
) = Npc(
    id = id,
    name = name,
    status = status,
    lifeState = lifeState,
    description = "",
    locationIds = locationIds,
    race = race,
    faction = faction,
)

internal fun quest(
    id: Long,
    title: String = "Quest $id",
    status: QuestStatus = QuestStatus.ACTIVE,
    givenByNpcId: Long? = null,
    locationId: Long? = null,
) = Quest(
    id = id,
    title = title,
    status = status,
    reward = "",
    givenByNpcId = givenByNpcId,
    locationId = locationId,
)
