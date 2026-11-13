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
}
