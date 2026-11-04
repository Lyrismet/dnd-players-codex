package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharacterSheetRepositoryTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val sheets = CharacterSheetRepositoryImpl(db.database, db.current)
    private val resources = CharacterResourceRepositoryImpl(db.database, db.current)
    private val rest = CharacterRestRepositoryImpl(db.database)

    private suspend fun lira(): Long =
        party.upsert(
            PartyMember(
                id = 0,
                name = "Lira",
                characterClass = "Wizard",
                race = "",
                level = 5,
                playerName = "",
                isPlayerCharacter = true,
                presence = PartyPresence.IN,
                hpMax = 28,
                hpCurrent = 22,
                description = "",
            ),
        )

    @Test
    fun `a member without a sheet reads as an empty one`() =
        runTest {
            val id = lira()
            assertEquals(CharacterSheet(), sheets.getSheet(id).copy(originId = ""))
        }

    @Test
    fun `a sheet keeps its scores marks purse and texts`() =
        runTest {
            val id = lira()
            sheets.saveSheet(
                id,
                CharacterSheet(
                    abilities = mapOf(Ability.INTELLIGENCE to 18, Ability.STRENGTH to 8),
                    saves = setOf(Ability.INTELLIGENCE),
                    skillMarks = mapOf(Skill.HISTORY to Proficiency.EXPERTISE, Skill.ARCANA to Proficiency.PROFICIENT),
                    skillAdjustments = mapOf(Skill.STEALTH to -2),
                    spellAbility = Ability.INTELLIGENCE,
                    speed = 30,
                    inspiration = true,
                    subclass = "Evoker",
                    purse = CoinPurse(gold = 34, silver = 18, copper = 25),
                ),
            )
            val stored = sheets.getSheet(id)
            assertEquals(mapOf(Ability.INTELLIGENCE to 18, Ability.STRENGTH to 8), stored.abilities)
            assertEquals(setOf(Ability.INTELLIGENCE), stored.saves)
            assertEquals(Proficiency.EXPERTISE, stored.mark(Skill.HISTORY))
            assertEquals(Proficiency.PROFICIENT, stored.mark(Skill.ARCANA))
            assertEquals(-2, stored.adjustment(Skill.STEALTH))
            assertEquals(Ability.INTELLIGENCE, stored.spellAbility)
            assertEquals(30, stored.speed)
            assertTrue(stored.inspiration)
            assertEquals("Evoker", stored.subclass)
            assertEquals(CoinPurse(gold = 34, silver = 18, copper = 25), stored.purse)
            assertTrue(stored.originId.isNotBlank())
        }

    @Test
    fun `saving a sheet twice updates it and keeps its origin id`() =
        runTest {
            val id = lira()
            sheets.saveSheet(id, CharacterSheet(speed = 30))
            val origin = sheets.getSheet(id).originId
            sheets.saveSheet(id, CharacterSheet(speed = 35, originId = origin))
            assertEquals(35, sheets.getSheet(id).speed)
            assertEquals(origin, sheets.getSheet(id).originId)
        }

    @Test
    fun `scores and coins are held in range`() =
        runTest {
            val id = lira()
            sheets.saveSheet(
                id,
                CharacterSheet(abilities = mapOf(Ability.WISDOM to 99), purse = CoinPurse(gold = 99_999_999)),
            )
            val stored = sheets.getSheet(id)
            assertEquals(30, stored.abilities[Ability.WISDOM])
            assertEquals(9_999_999, stored.purse.gold)
        }

    @Test
    fun `vitals are written without touching the rest of the member`() =
        runTest {
            val id = lira()
            sheets.saveVitals(
                id,
                CharacterVitals(hpMax = 28, hpCurrent = 10, tempHp = 3, armorClass = 12, armorClassFrom = "robe"),
            )
            val vitals = sheets.observeVitals(id).first()
            assertEquals(10, vitals?.hpCurrent)
            assertEquals(3, vitals?.tempHp)
            assertEquals(12, vitals?.armorClass)
            assertEquals("robe", vitals?.armorClassFrom)
            assertEquals("Lira", party.getById(id)?.name)
            assertTrue(party.getById(id)?.isPlayerCharacter == true)
        }

    @Test
    fun `resources keep their counts and slots sort after features`() =
        runTest {
            val id = lira()
            resources.upsert(id, CharacterResource(0, "Slots 2", max = 2, current = 2, slotLevel = 2))
            resources.upsert(id, CharacterResource(0, "Rage", max = 3, current = 3, position = 1))
            resources.upsert(id, CharacterResource(0, "Slots 1", max = 4, current = 4, slotLevel = 1))
            assertEquals(listOf("Rage", "Slots 1", "Slots 2"), resources.observe(id).first().map { it.name })
            val rage = resources.observe(id).first().first()
            resources.setCurrent(rage.id, 9)
            assertEquals(
                3,
                resources
                    .observe(id)
                    .first()
                    .first()
                    .current,
            )
            resources.setCurrent(rage.id, 1)
            assertEquals(
                1,
                resources
                    .observe(id)
                    .first()
                    .first()
                    .current,
            )
            resources.delete(rage.id)
            assertEquals(2, resources.observe(id).first().size)
        }

    @Test
    fun `a long rest changes the member and the resources and the undo puts back only those`() =
        runTest {
            val id = lira()
            sheets.saveVitals(id, CharacterVitals(hpMax = 28, hpCurrent = 22, tempHp = 3, deathSavesFailure = 1))
            val rage = resources.upsert(id, CharacterResource(0, "Rage", max = 2, current = 0))
            resources.upsert(id, CharacterResource(0, "Slots", max = 3, current = 1, slotLevel = 1))

            val plan = assertNotNull(rest.longRest(id))
            assertEquals(28, plan.summary.hpAfter)
            val rested = assertNotNull(sheets.observeVitals(id).first())
            assertEquals(28, rested.hpCurrent)
            assertEquals(0, rested.tempHp)
            assertEquals(0, rested.deathSavesFailure)
            assertEquals(listOf(2, 3), resources.observe(id).first().map { it.current })

            sheets.saveSheet(id, CharacterSheet(subclass = "Evoker"))
            rest.undo(id, plan.undo)
            assertEquals(22, sheets.observeVitals(id).first()?.hpCurrent)
            assertEquals(3, sheets.observeVitals(id).first()?.tempHp)
            assertEquals(listOf(0, 1), resources.observe(id).first().map { it.current })
            assertEquals("Evoker", sheets.getSheet(id).subclass)
            assertEquals(
                rage,
                resources
                    .observe(id)
                    .first()
                    .first()
                    .id,
            )
        }

    @Test
    fun `a rest of a member that is gone does nothing`() =
        runTest {
            assertNull(rest.longRest(404))
            assertNull(rest.preview(404))
        }

    @Test
    fun `deleting the member removes the sheet and the resources with it`() =
        runTest {
            val id = lira()
            sheets.saveSheet(id, CharacterSheet(speed = 30))
            resources.upsert(id, CharacterResource(0, "Rage", max = 2, current = 2))
            party.delete(id)
            assertEquals(
                0L,
                db.database.characterSheetQueries
                    .selectByMember(id)
                    .executeAsList()
                    .size
                    .toLong(),
            )
            assertTrue(resources.observe(id).first().isEmpty())
        }
}
