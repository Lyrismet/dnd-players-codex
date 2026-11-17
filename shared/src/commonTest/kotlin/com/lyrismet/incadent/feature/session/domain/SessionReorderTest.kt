package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun at(
    id: Long,
    day: Int,
    second: Int = 0,
) = SessionNote(id, "S$id", LocalDateTime(2026, 3, day, 12, 0, second), endedAt = null)

private fun List<SessionNote>.after(changes: List<SessionDateChange>): List<SessionNote> =
    map { note -> changes.firstOrNull { it.id == note.id }?.let { note.copy(sessionDate = it.to) } ?: note }

private fun List<SessionNote>.idsByDate(): List<Long> = sortedBy { it.sessionDate }.map { it.id }

class SessionReorderTest {
    private val sessions = listOf(at(1, 1), at(2, 11), at(3, 21), at(4, 31 - 2))

    @Test
    fun `a session moved between two others gets a date between them`() {
        val changes = sessions.moveChanges(movedId = 1, toIndex = 2)

        assertEquals(listOf(1L), changes.map { it.id })
        assertEquals(listOf(2L, 3L, 1L, 4L), sessions.after(changes).idsByDate())
        assertTrue(changes.single().to > sessions[2].sessionDate && changes.single().to < sessions[3].sessionDate)
    }

    @Test
    fun `a session put in the place it already has changes nothing`() {
        assertTrue(sessions.moveChanges(movedId = 2, toIndex = 1).isEmpty())
    }

    @Test
    fun `the oldest place is a day before the old first session`() {
        val changes = sessions.moveChanges(movedId = 3, toIndex = 0)

        assertEquals(listOf(3L, 1L, 2L, 4L), sessions.after(changes).idsByDate())
    }

    @Test
    fun `the newest place is a day after the last session`() {
        val changes = sessions.moveChanges(movedId = 1, toIndex = 3)

        assertEquals(listOf(2L, 3L, 4L, 1L), sessions.after(changes).idsByDate())
    }

    @Test
    fun `equal dates of the neighbours push the later sessions on by a second`() {
        val tied = listOf(at(1, 1), at(2, 5), at(3, 5), at(4, 9))

        val changes = tied.moveChanges(movedId = 1, toIndex = 1)

        val moved = tied.after(changes)
        assertEquals(listOf(2L, 1L, 3L, 4L), moved.idsByDate())
        assertEquals(moved.size, moved.map { it.sessionDate }.toSet().size)
    }

    @Test
    fun `the numbers follow the new order`() {
        val changes = sessions.moveChanges(movedId = 1, toIndex = 2)

        val labels = sessions.after(changes).sessionNumberLabels(SessionNumbering.ARABIC)

        assertEquals(mapOf(2L to "1", 3L to "2", 1L to "3", 4L to "4"), labels)
    }

    @Test
    fun `a gap under two seconds gives the new session the second after its earlier neighbour`() {
        val others = listOf(at(1, 5, 0), at(2, 5, 1))

        val placement = placeAmong(others, index = 1)

        assertEquals(LocalDateTime(2026, 3, 5, 12, 0, 1), placement.date)
        assertEquals(mapOf(2L to LocalDateTime(2026, 3, 5, 12, 0, 2)), placement.shifted)
    }
}
