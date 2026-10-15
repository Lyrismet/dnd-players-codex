package com.lyrismet.incadent.feature.session.data

import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val moment = LocalDateTime(2026, 1, 1, 12, 0)

private fun note(title: String) = SessionNote(id = 0, title = title, sessionDate = moment, endedAt = null)

class SessionRepositoriesTest {
    private val db = TestDatabase()
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val entries = SessionEntryRepositoryImpl(db.database, db.current)

    @Test
    fun `every inserted note and entry gets its own id`() =
        runTest {
            val first = notes.upsert(note("A"))
            val second = notes.upsert(note("B"))
            val entryOne = entries.add(first, "раз")
            val entryTwo = entries.add(first, "два")

            assertNotEquals(first, second)
            assertNotEquals(entryOne, entryTwo)
            assertEquals(second, notes.getById(second)?.id)
        }

    @Test
    fun `notes and entries are scoped to the open campaign`() =
        runTest {
            val mine = notes.upsert(note("Моя"))
            entries.add(mine, "запись")
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)
            val theirs = notes.upsert(note("Чужая"))
            entries.add(theirs, "чужая запись")

            assertEquals(listOf("Чужая"), notes.observeAll().first().map { it.title })
            assertEquals(listOf("чужая запись"), entries.observeAll().first().map { it.body })

            db.current.open(TestDatabase.FIRST_CAMPAIGN)
            assertEquals(listOf("Моя"), notes.observeAll().first().map { it.title })
        }

    @Test
    fun `making a session the only live one ends the others and leaves other campaigns alone`() =
        runTest {
            val old = notes.upsert(note("Старая"))
            val fresh = notes.upsert(note("Новая"))
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)
            val foreign = notes.upsert(note("Другая кампания"))
            db.current.open(TestDatabase.FIRST_CAMPAIGN)

            notes.makeOnlyLive(fresh, moment)

            assertTrue(checkNotNull(notes.getById(old)).endedAt != null)
            assertTrue(checkNotNull(notes.getById(fresh)).isLive)
            assertTrue(checkNotNull(notes.getById(foreign)).isLive)
        }

    @Test
    fun `resuming an ended session reopens it and ends the live one`() =
        runTest {
            val ended = notes.upsert(SessionNote(0, "Закончена", moment, endedAt = moment))
            val live = notes.upsert(note("Идёт"))

            notes.makeOnlyLive(ended, moment)

            assertTrue(checkNotNull(notes.getById(ended)).isLive)
            assertTrue(!checkNotNull(notes.getById(live)).isLive)
        }

    @Test
    fun `deleting a note removes its entries and deleting a campaign removes its notes`() =
        runTest {
            val doomed = notes.upsert(note("Удаляемая"))
            entries.add(doomed, "запись")
            val kept = notes.upsert(note("Остаётся"))
            entries.add(kept, "другая запись")

            notes.delete(doomed)
            assertEquals(listOf("другая запись"), entries.observeAll().first().map { it.body })

            // the foreign keys pragma is what makes this cascade fire
            db.database.campaignQueries.deleteById(TestDatabase.FIRST_CAMPAIGN)
            assertEquals(
                0L,
                db.database.sessionNoteQueries
                    .selectById(kept)
                    .executeAsList()
                    .size
                    .toLong(),
            )
        }
}
