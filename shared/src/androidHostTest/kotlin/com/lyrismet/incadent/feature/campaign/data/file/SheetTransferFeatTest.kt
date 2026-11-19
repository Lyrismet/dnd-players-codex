package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.codex.data.PartyRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.FeatSource
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetTransferFeatTest {
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
    fun `a feat written from a file is read back and cleared with the sheet`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Feat(0, "Darkvision", FeatSource.RACE.name, "sees in the dark", 2),
            )

            val back = readSheetRecords(db.database, id, 7).filterIsInstance<FileRecord.Feat>().single()
            assertEquals("Darkvision", back.name)
            assertEquals(7, back.partyId)
            assertEquals(FeatSource.RACE.name, back.source)
            assertEquals(2, back.position)

            clearSheet(db.database, id)
            assertTrue(readSheetRecords(db.database, id, 7).none { it is FileRecord.Feat })
        }

    @Test
    fun `a feat with an unknown source and a negative position is pinned`() =
        runTest {
            val id = member()
            writeSheetRecord(
                db.database,
                db.current.requireId(),
                id,
                FileRecord.Feat(0, "Odd", source = "???", position = -4),
            )

            val back = readSheetRecords(db.database, id, 1).filterIsInstance<FileRecord.Feat>().single()
            assertEquals("", back.source)
            assertEquals(0, back.position)
        }
}
