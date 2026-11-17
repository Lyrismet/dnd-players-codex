package com.lyrismet.incadent.feature.session.data

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.data.CharacterSheetRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.ItemRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.PartyRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.session.domain.DowntimeLedger
import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.model.DowntimeItem
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DowntimeTest {
    private val db = TestDatabase()
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val downtimes = DowntimeRepositoryImpl(db.database, db.current)
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val sheets = CharacterSheetRepositoryImpl(db.database, db.current)
    private val items = ItemRepositoryImpl(db.database, db.current)
    private val ledger = DowntimeLedger(sheets, items)

    private suspend fun session(day: Int): Long =
        notes.upsert(SessionNote(0, "S$day", LocalDateTime(2026, 1, day, 12, 0), endedAt = null))

    private suspend fun lira(): Long =
        party.upsert(
            PartyMember(
                id = 0,
                name = "Lira",
                characterClass = "",
                race = "",
                level = 1,
                playerName = "",
                isPlayerCharacter = true,
                presence = PartyPresence.IN,
                hpMax = 10,
                hpCurrent = 10,
                description = "",
            ),
        )

    private fun loot(after: Long) =
        Downtime(
            id = 0,
            afterSessionId = after,
            shortText = "Кузница",
            goldDelta = 120,
            items = listOf(DowntimeItem("Зелье лечения", 2)),
        )

    @Test
    fun `a downtime keeps its texts gold and items`() =
        runTest {
            val after = session(1)
            val id = downtimes.upsert(loot(after).copy(longText = "долго", gainedText = "меч"))

            val stored = downtimes.getById(id)

            assertEquals("Кузница", stored?.shortText)
            assertEquals("долго", stored?.longText)
            assertEquals(120, stored?.goldDelta)
            assertEquals(listOf(DowntimeItem("Зелье лечения", 2)), stored?.items)
            assertNull(stored?.appliedTo)
            assertEquals(listOf(id), downtimes.observeAll().first().map { it.id })
        }

    @Test
    fun `a gap holds one downtime`() =
        runTest {
            val after = session(1)
            downtimes.upsert(loot(after))

            assertFailsWith<Exception> { downtimes.upsert(loot(after)) }
        }

    @Test
    fun `deleting the session takes its downtime along`() =
        runTest {
            val after = session(1)
            downtimes.upsert(loot(after))

            notes.delete(after)

            assertTrue(downtimes.observeAll().first().isEmpty())
        }

    @Test
    fun `applying puts the gold and the items into the sheet`() =
        runTest {
            val member = lira()
            val id = downtimes.upsert(loot(session(1)))

            val applied = ledger.apply(downtimes.getById(id)!!, member)
            downtimes.upsert(applied)

            assertEquals(120, sheets.getSheet(member).purse.gold)
            val item = items.observeAll().first().single()
            assertEquals("Зелье лечения", item.name)
            assertEquals(2, item.quantity)
            assertEquals(ItemOwner.Of(EntityKind.PARTY, member), item.owner)
            assertEquals(member, downtimes.getById(id)?.appliedTo)
            assertEquals(
                item.id,
                downtimes
                    .getById(id)
                    ?.items
                    ?.single()
                    ?.itemId,
            )
        }

    @Test
    fun `taking back removes only what was put in`() =
        runTest {
            val member = lira()
            sheets.updateSheet(member) { it.copy(purse = CoinPurse(gold = 50)) }
            val id = downtimes.upsert(loot(session(1)))
            downtimes.upsert(ledger.apply(downtimes.getById(id)!!, member))

            val reverted = ledger.revert(downtimes.getById(id)!!)
            downtimes.upsert(reverted)

            assertEquals(50, sheets.getSheet(member).purse.gold)
            assertTrue(items.observeAll().first().isEmpty())
            assertNull(downtimes.getById(id)?.appliedTo)
        }

    @Test
    fun `taking back leaves the rest of a stack that grew`() =
        runTest {
            val member = lira()
            val id = downtimes.upsert(loot(session(1)))
            downtimes.upsert(ledger.apply(downtimes.getById(id)!!, member))
            items.stepQuantity(
                items
                    .observeAll()
                    .first()
                    .single()
                    .id,
                3,
            )

            ledger.revert(downtimes.getById(id)!!)

            assertEquals(
                3,
                items
                    .observeAll()
                    .first()
                    .single()
                    .quantity,
            )
        }

    @Test
    fun `spent gold cannot take the purse below zero`() =
        runTest {
            val member = lira()
            sheets.updateSheet(member) { it.copy(purse = CoinPurse(gold = 30)) }
            val id = downtimes.upsert(Downtime(0, session(1), shortText = "Пир", goldDelta = -100))

            ledger.apply(downtimes.getById(id)!!, member)

            assertEquals(0, sheets.getSheet(member).purse.gold)
        }

    @Test
    fun `a downtime is scoped to the open campaign`() =
        runTest {
            downtimes.upsert(loot(session(1)))
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)

            assertTrue(downtimes.observeAll().first().isEmpty())
        }

    @Test
    fun `setting dates moves sessions and an undo with the old dates puts them back`() =
        runTest {
            val first = session(1)
            val second = session(2)
            val old = notes.getById(first)!!.sessionDate
            val moved = LocalDateTime(2026, 1, 5, 12, 0)

            notes.setDates(mapOf(first to moved))
            assertEquals(
                listOf(second, first),
                notes
                    .observeAll()
                    .first()
                    .sortedBy { it.sessionDate }
                    .map { it.id },
            )

            notes.setDates(mapOf(first to old))
            assertEquals(old, notes.getById(first)?.sessionDate)
        }

    @Test
    fun `a downtime stays with its session when the session is moved`() =
        runTest {
            val first = session(1)
            session(2)
            val id = downtimes.upsert(loot(first))

            notes.setDates(mapOf(first to LocalDateTime(2026, 1, 9, 12, 0)))

            assertEquals(first, downtimes.getById(id)?.afterSessionId)
        }
}
