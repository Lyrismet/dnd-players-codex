package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartyHpMaxApproxTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)

    private suspend fun tarn(): PartyMember {
        val id =
            party.upsert(
                PartyMember(
                    id = 0,
                    name = "Tarn",
                    characterClass = "",
                    race = "",
                    level = 3,
                    playerName = "",
                    isPlayerCharacter = false,
                    presence = PartyPresence.IN,
                    hpMax = 30,
                    hpCurrent = 30,
                    description = "",
                ),
            )
        return checkNotNull(party.getById(id))
    }

    @Test
    fun `a new member has an exact maximum`() =
        runTest {
            assertFalse(tarn().hpMaxApprox)
        }

    @Test
    fun `a level that rose over the same maximum makes it approximate`() =
        runTest {
            val member = tarn()
            party.upsert(member.copy(level = 4))
            assertTrue(checkNotNull(party.getById(member.id)).hpMaxApprox)
        }

    @Test
    fun `a new maximum typed with the level makes it exact again`() =
        runTest {
            val member = tarn()
            party.upsert(member.copy(level = 4))
            val guessed = checkNotNull(party.getById(member.id))
            party.upsert(guessed.copy(hpMax = 36))
            assertFalse(checkNotNull(party.getById(member.id)).hpMaxApprox)
        }

    @Test
    fun `saving other fields keeps the guess`() =
        runTest {
            val member = tarn()
            party.upsert(member.copy(level = 4))
            val guessed = checkNotNull(party.getById(member.id))
            party.upsert(guessed.copy(description = "brave"))
            assertEquals(true, party.getById(member.id)?.hpMaxApprox)
        }

    @Test
    fun `a lower level or the same one leaves the maximum exact`() =
        runTest {
            val member = tarn()
            party.upsert(member.copy(level = 3))
            party.upsert(member.copy(level = 2))
            assertFalse(checkNotNull(party.getById(member.id)).hpMaxApprox)
        }
}
