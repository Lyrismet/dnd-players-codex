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
}
