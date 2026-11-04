package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemRepositoryTest {
    private val db = TestDatabase()
    private val items = ItemRepositoryImpl(db.database, db.current)

    @Test
    fun `an item is stored with its price coin and owner`() =
        runTest {
            val owner = ItemOwner.Of(EntityKind.NPC, 4)
            val id =
                items.upsert(
                    Item(0, "Potion", quantity = 2, price = 50, coin = Coin.GOLD, owner = owner, notes = "n"),
                )
            val stored = items.getById(id)
            assertEquals("Potion", stored?.name)
            assertEquals(2, stored?.quantity)
            assertEquals(50, stored?.price)
            assertEquals(owner, stored?.owner)
            assertTrue(stored?.originId.orEmpty().isNotBlank())
        }

    @Test
    fun `quantity and price are held in range`() =
        runTest {
            val id = items.upsert(Item(0, "Gem", quantity = 50_000, price = 99_999_999))
            assertEquals(9999, items.getById(id)?.quantity)
            assertEquals(9_999_999, items.getById(id)?.price)
            items.upsert(items.getById(id)!!.copy(quantity = 0))
            assertEquals(1, items.getById(id)?.quantity)
        }

    @Test
    fun `an item without a price or owner reads back shared`() =
        runTest {
            val id = items.upsert(Item(0, "Rope"))
            assertNull(items.getById(id)?.price)
            assertEquals(ItemOwner.Shared, items.getById(id)?.owner)
        }

    @Test
    fun `a deleted item comes back under its old id`() =
        runTest {
            val id = items.upsert(Item(0, "Lamp", quantity = 3))
            val stored = items.getById(id)!!
            items.delete(id)
            assertNull(items.getById(id))
            assertEquals(id, items.upsert(stored))
            assertEquals(3, items.getById(id)?.quantity)
        }

    @Test
    fun `handing items over changes only their owner`() =
        runTest {
            val first = items.upsert(Item(0, "Sword", quantity = 2))
            val second = items.upsert(Item(0, "Shield"))
            val owner = ItemOwner.Of(EntityKind.PARTY, 9)
            items.setOwner(listOf(first, second), owner)
            val all = items.observeAll().first()
            assertEquals(listOf(owner, owner), all.map { it.owner })
            assertEquals(2, all.first { it.id == first }.quantity)
        }
}
