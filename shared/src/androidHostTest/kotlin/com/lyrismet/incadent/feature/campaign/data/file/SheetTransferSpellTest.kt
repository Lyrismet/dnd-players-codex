package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.codex.data.PartyRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetTransferSpellTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)

    private suspend fun member(): Long =
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
    fun `a spell written from a file is read back whole and cleared with the sheet`() =
        runTest {
            val id = member()
            val spell =
                FileRecord.Spell(
                    0,
                    "Misty Step",
                    2,
                    "Conjuration",
                    concentration = true,
                    freeMax = 2,
                    freeLeft = 1,
                )
            writeSheetRecord(db.database, db.current.requireId(), id, spell)

            val back = readSheetRecords(db.database, id, 7).filterIsInstance<FileRecord.Spell>().single()
            assertEquals("Misty Step", back.name)
            assertEquals(7, back.partyId)
            assertEquals(1, back.freeLeft)
            assertTrue(back.concentration)

            clearSheet(db.database, id)
            assertTrue(readSheetRecords(db.database, id, 7).none { it is FileRecord.Spell })
        }

    @Test
    fun `a spell with numbers out of range is pinned`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Spell(0, "Wish", level = 40, freeMax = 50, freeLeft = 99),
            )
            val back = readSheetRecords(db.database, id, 1).filterIsInstance<FileRecord.Spell>().single()
            assertEquals(9, back.level)
            assertEquals(9, back.freeMax)
            assertEquals(9, back.freeLeft)
        }

    @Test
    fun `the time the duration and the damage go through a file`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Spell(
                    0,
                    "Ice Knife",
                    1,
                    castKind = "BONUS",
                    durationUnit = "MINUTE",
                    durationValue = 1,
                    damage = "PIERCING:1d10;COLD:2d6",
                    projectiles = 3,
                ),
            )
            val back = readSheetRecords(db.database, id, 1).filterIsInstance<FileRecord.Spell>().single()
            assertEquals("BONUS", back.castKind)
            assertEquals("MINUTE", back.durationUnit)
            assertEquals(1, back.durationValue)
            assertEquals("PIERCING:1d10;COLD:2d6", back.damage)
            assertEquals(3, back.projectiles)
        }

    @Test
    fun `an unknown kind or unit from a file falls back`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Spell(0, "Odd", castKind = "???", durationUnit = "???", projectiles = 99),
            )
            val back = readSheetRecords(db.database, id, 1).filterIsInstance<FileRecord.Spell>().single()
            assertEquals("ACTION", back.castKind)
            assertEquals("NONE", back.durationUnit)
            assertEquals(20, back.projectiles)
        }

    @Test
    fun `the growth goes through a file`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Spell(0, "Cantrip", growth = "1~DICE~DICE,EVERY,0,2,1d6,"),
            )
            val back = readSheetRecords(db.database, id, 1).filterIsInstance<FileRecord.Spell>().single()
            assertEquals("1~DICE~DICE,EVERY,0,2,1d6,", back.growth)
        }
}
