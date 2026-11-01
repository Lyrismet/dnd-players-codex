package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomCreatureTypeTest {
    private val db = TestDatabase()
    private val repository = SpeciesRepositoryImpl(db.database, db.current)

    private suspend fun speciesOf(type: SpeciesType): Long =
        repository.upsert(Species(0, "Шестерня", CreatureType.UNSPECIFIED).withType(type))

    @Test
    fun `a created type is written in lower case and the same name gives the same type`() =
        runTest {
            val first = repository.createCustomType("  Механизм ")
            val second = repository.createCustomType("механизм")

            assertEquals("механизм", first.name)
            assertEquals(first.id, second.id)
            assertEquals(1, repository.observeCustomTypes().first().size)
        }

    @Test
    fun `a species keeps its own type and shows the renamed one`() =
        runTest {
            val type = repository.createCustomType("механизм")
            val id = speciesOf(SpeciesType.Custom(type))

            repository.renameCustomType(type.id, "Автомат")

            assertEquals("автомат", repository.getById(id)?.customType?.name)
            assertEquals(SpeciesType.Custom(type.copy(name = "автомат")), repository.getById(id)?.type)
        }

    @Test
    fun `a species read before its type was deleted is stored without the type`() =
        runTest {
            val type = repository.createCustomType("механизм")
            val id = speciesOf(SpeciesType.Custom(type))
            val snapshot = checkNotNull(repository.getById(id))
            repository.delete(id)
            repository.deleteCustomType(type.id)

            repository.upsert(snapshot)

            assertEquals(null, repository.getById(id)?.customType)
        }

    @Test
    fun `a rename onto another type is refused`() =
        runTest {
            val first = repository.createCustomType("механизм")
            repository.createCustomType("автомат")

            repository.renameCustomType(first.id, "автомат")

            assertEquals(
                listOf("автомат", "механизм"),
                repository
                    .observeCustomTypes()
                    .first()
                    .map { it.name },
            )
        }

    @Test
    fun `deleting a type leaves the species with no type`() =
        runTest {
            val type = repository.createCustomType("механизм")
            val id = speciesOf(SpeciesType.Custom(type))

            repository.deleteCustomType(type.id)

            val species = assertNotNull(repository.getById(id))
            assertEquals(SpeciesType.None, species.type)
            assertEquals("Шестерня", species.name)
            assertTrue(repository.observeCustomTypes().first().isEmpty())
        }
}
