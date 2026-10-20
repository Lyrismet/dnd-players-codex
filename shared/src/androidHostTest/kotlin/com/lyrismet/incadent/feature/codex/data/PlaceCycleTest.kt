package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.repository.PlaceCycleException
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaceCycleTest {
    private val db = TestDatabase()
    private val repository = LocationRepositoryImpl(db.database, db.current)

    @Test
    fun `a place can't be saved inside itself or inside its descendant`() =
        runTest {
            val top = repository.upsert(Location(id = 0, name = "Верх", description = ""))
            val mid = repository.upsert(Location(id = 0, name = "Середина", description = "", parentId = top))
            val low = repository.upsert(Location(id = 0, name = "Низ", description = "", parentId = mid))
            val stored = repository.observeAll().first().associateBy { it.id }

            assertFailsWith<PlaceCycleException> { repository.upsert(stored.getValue(top).copy(parentId = top)) }
            assertFailsWith<PlaceCycleException> { repository.upsert(stored.getValue(top).copy(parentId = low)) }
            assertEquals(null, repository.getById(top)?.parentId)
        }

    @Test
    fun `a place moves to another branch and to the top level`() =
        runTest {
            val a = repository.upsert(Location(id = 0, name = "А", description = ""))
            val b = repository.upsert(Location(id = 0, name = "Б", description = "", parentId = a))
            val c = repository.upsert(Location(id = 0, name = "В", description = ""))

            repository.upsert(checkNotNull(repository.getById(b)).copy(parentId = c))
            assertEquals(c, repository.getById(b)?.parentId)
            repository.upsert(checkNotNull(repository.getById(b)).copy(parentId = null))
            assertEquals(null, repository.getById(b)?.parentId)
        }

    @Test
    fun `a refused move in a batch leaves every place where it was`() =
        runTest {
            val a = repository.upsert(Location(id = 0, name = "А", description = ""))
            val b = repository.upsert(Location(id = 0, name = "Б", description = ""))
            val c = repository.upsert(Location(id = 0, name = "В", description = "", parentId = b))

            assertFailsWith<PlaceCycleException> { repository.setParents(linkedMapOf(a to b, b to c)) }

            assertEquals(null, repository.getById(a)?.parentId)
            assertEquals(null, repository.getById(b)?.parentId)
        }

    @Test
    fun `set inside makes a place hold exactly the picked ones`() =
        runTest {
            val home = repository.upsert(Location(id = 0, name = "Дом", description = ""))
            val kept = repository.upsert(Location(id = 0, name = "Кухня", description = "", parentId = home))
            val dropped = repository.upsert(Location(id = 0, name = "Подвал", description = "", parentId = home))
            val added = repository.upsert(Location(id = 0, name = "Сад", description = ""))

            val changes = repository.setInside(home, listOf(kept, added))

            assertEquals(setOf(added), changes.entering)
            assertEquals(setOf(dropped), changes.leaving)
            assertEquals(home, repository.getById(added)?.parentId)
            assertEquals(null, repository.getById(dropped)?.parentId)
            assertEquals(home, repository.getById(kept)?.parentId)
        }

    @Test
    fun `a loop through a deep chain is refused`() =
        runTest {
            val ids = ArrayList<Long>()
            repeat(DEEP_LEVELS) { level ->
                ids +=
                    repository.upsert(
                        Location(id = 0, name = "Уровень $level", description = "", parentId = ids.lastOrNull()),
                    )
            }

            assertFailsWith<PlaceCycleException> { repository.setParent(ids.first(), ids.last()) }
        }

    private companion object {
        const val DEEP_LEVELS = 15
    }
}
