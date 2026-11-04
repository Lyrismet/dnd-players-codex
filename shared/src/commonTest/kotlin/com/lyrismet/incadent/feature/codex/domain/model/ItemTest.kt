package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.carriedBy
import com.lyrismet.incadent.feature.codex.domain.grouping.equipmentChanges
import com.lyrismet.incadent.feature.codex.domain.grouping.optionId
import com.lyrismet.incadent.feature.codex.domain.grouping.ownerOfOptionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ItemTest {
    private val potion = Item(id = 1, name = "Potion")

    @Test
    fun `quantity is held between 1 and 9999`() {
        assertEquals(1, potion.withQuantity(0).quantity)
        assertEquals(9999, potion.withQuantity(10_000).quantity)
        assertEquals(9999, potion.copy(quantity = 9999).withOneMore().quantity)
        assertEquals(2, potion.withOneMore().quantity)
    }

    @Test
    fun `one less on the last piece removes the item`() {
        assertNull(potion.withOneLess())
        assertEquals(1, potion.copy(quantity = 2).withOneLess()?.quantity)
    }

    @Test
    fun `option ids round trip for every kind of owner`() {
        val owners =
            listOf(
                ItemOwner.Shared,
                ItemOwner.Of(EntityKind.PARTY, 5),
                ItemOwner.Of(EntityKind.NPC, 5),
                ItemOwner.Of(EntityKind.LOCATION, 12),
            )
        owners.forEach { assertEquals(it, ownerOfOptionId(it.optionId())) }
        assertEquals(4, owners.map { it.optionId() }.toSet().size)
    }

    @Test
    fun `gear pick moves ticked items and frees the ones it dropped`() {
        val lira = ItemOwner.Of(EntityKind.PARTY, 1)
        val bram = ItemOwner.Of(EntityKind.NPC, 2)
        val items =
            listOf(
                Item(1, "Sword", owner = lira),
                Item(2, "Rope", owner = bram),
                Item(3, "Lamp"),
                Item(4, "Map", owner = bram),
            )
        val changes = equipmentChanges(items, lira, selected = setOf(2, 3))
        assertEquals(mapOf(1L to ItemOwner.Shared, 2L to lira, 3L to lira), changes)
    }

    @Test
    fun `gear pick that changes nothing returns nothing`() {
        val lira = ItemOwner.Of(EntityKind.PARTY, 1)
        val items = listOf(Item(1, "Sword", owner = lira), Item(2, "Lamp"))
        assertEquals(emptyMap(), equipmentChanges(items, lira, selected = setOf(1)))
    }

    @Test
    fun `carried items are listed by name`() {
        val lira = ItemOwner.Of(EntityKind.PARTY, 1)
        val items = listOf(Item(1, "Rope", owner = lira), Item(2, "axe", owner = lira), Item(3, "Lamp"))
        assertEquals(listOf("axe", "Rope"), items.carriedBy(lira).map { it.name })
    }
}
