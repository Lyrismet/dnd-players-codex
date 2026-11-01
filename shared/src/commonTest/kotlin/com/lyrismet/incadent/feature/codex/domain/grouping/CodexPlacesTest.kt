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
    fun `the top level places are never regions, the places just below them are`() {
        assertEquals(valley, tree.regionOf(temple))
        assertEquals(valley, tree.regionOf(valley))
        assertEquals(castle, tree.regionOf(castle))
        assertEquals(tower, tree.regionOf(tower))
        assertEquals(null, tree.regionOf(plane))
        assertEquals(null, tree.regionOf(wastes))
    }

    @Test
    fun `two planes share their countries as regions`() {
        val material = location(1, "Материальный план")
        val ethereal = location(2, "Эфирный план")
        val sanChitlan = location(3, "Сан-Читлан", parentId = 1)
        val mist = location(4, "Туманы", parentId = 2)
        val village = location(5, "Деревня", parentId = 3)
        val planes = PlaceTree(listOf(material, ethereal, sanChitlan, mist, village))

        assertEquals(sanChitlan, planes.regionOf(village))
        assertEquals(mist, planes.regionOf(mist))
        assertEquals(null, planes.regionOf(material))
        assertEquals(null, planes.regionOf(ethereal))
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
        // the wrapper below the root is listed under the branching place, the root itself has no region
        assertEquals(plane, chained.regionOf(plane))
        assertEquals(null, chained.regionOf(world))
    }

    @Test
    fun `a lone top level place has no region`() {
        assertEquals(null, PlaceTree(listOf(plane)).regionOf(plane))
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

        assertEquals(listOf(castle, tower, valley), regions.regionsOfNpc(roamer))
        assertTrue(regions.regionsOfNpc(homeless).isEmpty())
    }

    @Test
    fun `a quest takes the region of its place and falls back to the first place of its giver`() {
        val giver = npc(1, locationIds = listOf(temple.id))
        val placed = quest(1, locationId = castle.id)
        val viaGiver = quest(2, givenByNpcId = giver.id)
        val nowhere = quest(3)
        val regions = CodexRegions(listOf(giver), all)

        assertEquals(castle, regions.regionOfQuest(placed))
        assertEquals(valley, regions.regionOfQuest(viaGiver))
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

    @Test
    fun `a place can't be the parent of itself or of anything inside it`() {
        assertFalse(tree.canPutInside(valley.id, valley.id))
        assertFalse(tree.canPutInside(valley.id, town.id))
        assertFalse(tree.canPutInside(valley.id, temple.id))
        assertTrue(tree.canPutInside(valley.id, wastes.id))
        assertTrue(tree.canPutInside(valley.id, null))
        assertTrue(tree.canPutInside(valley.id, plane.id))
    }

    @Test
    fun `blocked as parent names the place a descendant is inside of`() {
        val blocked = tree.blockedAsParent(valley.id)

        assertEquals(setOf(valley.id, town.id, temple.id), blocked.keys)
        assertEquals(PlaceBlock.Itself, blocked[valley.id])
        assertEquals(PlaceBlock.InsideOf("Долина"), blocked[temple.id])
    }

    @Test
    fun `blocked as child holds the place and the places it sits in`() {
        val blocked = tree.blockedAsChild(town.id)

        assertEquals(setOf(plane.id, valley.id, town.id), blocked.keys)
        assertEquals(PlaceBlock.Contains, blocked[plane.id])
        assertEquals(PlaceBlock.Itself, blocked[town.id])
    }

    @Test
    fun `a checked place inside another checked one travels with it`() {
        val checked = listOf(valley.id, temple.id, castle.id)

        assertEquals(listOf(valley.id, castle.id), tree.withoutCarried(checked))
        val changes = tree.insideChanges(plane.id, listOf(valley.id, temple.id))
        assertEquals(emptySet(), changes.entering)
        assertEquals(emptySet(), changes.leaving)
        val other = tree.insideChanges(wastes.id, listOf(castle.id, plane.id, town.id))
        assertEquals(setOf(plane.id), other.entering)
        assertEquals(setOf(tower.id), other.leaving)
    }

    @Test
    fun `pending moves are applied to the places before the rule is checked`() {
        // the valley will sit in the wastes and hold the tower, the town is left without a parent
        val pending = all.withPending(PlaceEditing(valley.id, valley.name, wastes.id, listOf(tower.id)))
        val after = PlaceTree(pending)

        assertEquals(listOf("Пустоши", "Долина"), after.pathOf(checkNotNull(after.find(valley.id))).map { it.name })
        assertEquals(
            listOf("Пустоши", "Долина", "Башня"),
            after.pathOf(checkNotNull(after.find(tower.id))).map { it.name },
        )
        assertEquals(null, after.find(town.id)?.parentId)
    }

    @Test
    fun `a place that is not saved yet joins the pending tree`() {
        val pending = all.withPending(PlaceEditing(NEW_PLACE_ID, "Новое", castle.id, emptyList()))

        assertTrue(NEW_PLACE_ID in PlaceTree(pending).descendantIds(castle.id))
    }

    @Test
    fun `a chain deeper than twelve places keeps its whole path`() {
        val chain = (1L..DEEP_PLACES).map { location(it, "П$it", parentId = if (it == 1L) null else it - 1) }
        val deepTree = PlaceTree(chain)

        assertEquals(DEEP_PLACES.toInt() - 1, deepTree.ancestorsOf(chain.last()).size)
        assertEquals(PlaceBlock.Contains, deepTree.blockedAsChild(DEEP_PLACES)[1L])
    }

    @Test
    fun `places stored inside a place stay marked as inside it while it is edited`() {
        val merged = tree.pendingBlocks(tree.editingOf(valley)) + tree.blockedAsParent(valley.id)

        assertEquals(PlaceBlock.InsideOf("Долина"), merged[town.id])
    }

    private companion object {
        const val DEEP_PLACES = 15L
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
) = Npc(
    id = id,
    name = name,
    status = status,
    lifeState = lifeState,
    description = "",
    locationIds = locationIds,
    race = race,
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
