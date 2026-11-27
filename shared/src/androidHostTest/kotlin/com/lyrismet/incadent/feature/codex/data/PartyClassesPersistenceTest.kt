package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.grouping.withClasses
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PartyClassesPersistenceTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)

    private val cleric = PartyClass("Жрец", "Домен Света", 3)
    private val fighter = PartyClass("Воин", "", 2)

    private val base =
        PartyMember(
            id = 0,
            name = "Лира",
            characterClass = "Жрец",
            race = "",
            level = 3,
            playerName = "",
            isPlayerCharacter = true,
            presence = PartyPresence.IN,
            hpMax = null,
            hpCurrent = null,
            description = "",
            subclass = "Домен Света",
        )

    @Test
    fun `a member without a list reads back with no classes`() =
        runTest {
            val id = party.upsert(base)
            assertEquals(emptyList(), party.getById(id)?.classes)
        }

    @Test
    fun `saving two classes stores the list and the summary of the first class and the sum`() =
        runTest {
            val id = party.upsert(base)
            party.upsert(assertNotNull(party.getById(id)).withClasses(listOf(cleric, fighter)))

            val stored = assertNotNull(party.getById(id))
            assertEquals(listOf(cleric, fighter), stored.classes)
            assertEquals("Жрец", stored.characterClass)
            assertEquals("Домен Света", stored.subclass)
            assertEquals(5, stored.level)
            assertEquals(
                listOf(cleric, fighter),
                party
                    .observeAll()
                    .first()
                    .single()
                    .classes,
            )
        }

    @Test
    fun `a list left stale by a hand edited summary is rewritten from the list`() =
        runTest {
            val id = party.upsert(base)
            val multi = assertNotNull(party.getById(id)).withClasses(listOf(cleric, fighter))
            party.upsert(multi.copy(characterClass = "Монах", level = 9))

            val stored = assertNotNull(party.getById(id))
            assertEquals("Жрец", stored.characterClass)
            assertEquals(5, stored.level)
        }

    @Test
    fun `going back to one class clears the list`() =
        runTest {
            val id = party.upsert(base)
            val multi = assertNotNull(party.getById(id)).withClasses(listOf(cleric, fighter))
            party.upsert(multi)
            party.upsert(multi.withClasses(listOf(fighter)))

            val stored = assertNotNull(party.getById(id))
            assertEquals(emptyList(), stored.classes)
            assertEquals("Воин", stored.characterClass)
            assertEquals(2, stored.level)
        }
}
