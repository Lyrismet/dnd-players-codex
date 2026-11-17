package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

private fun note(
    id: Long,
    day: Int,
) = SessionNote(
    id = id,
    title = "Сессия $id",
    sessionDate = LocalDateTime(2026, 9, day, 19, 0),
    endedAt = null,
)

class SessionNumberingTest {
    @Test
    fun `chronologicalIndex numbers sessions by date regardless of list order`() {
        // deliberately out of date order and out of id order
        val sessions = listOf(note(id = 20, day = 15), note(id = 10, day = 5), note(id = 30, day = 25))

        val index = sessions.chronologicalIndex()

        assertEquals(1, index.getValue(10))
        assertEquals(2, index.getValue(20))
        assertEquals(3, index.getValue(30))
    }

    @Test
    fun `chronologicalNumberLabels renders the index as a roman numeral`() {
        val sessions = listOf(note(id = 20, day = 15), note(id = 10, day = 5), note(id = 30, day = 25))

        val labels = sessions.chronologicalNumberLabels()

        assertEquals("I", labels.getValue(10))
        assertEquals("II", labels.getValue(20))
        assertEquals("III", labels.getValue(30))
    }

    @Test
    fun `sessionNumberLabels follows the chosen numbering style`() {
        val sessions = listOf(note(id = 20, day = 15), note(id = 10, day = 5), note(id = 30, day = 25))

        assertEquals("I", sessions.sessionNumberLabels(SessionNumbering.ROMAN).getValue(10))
        assertEquals("1", sessions.sessionNumberLabels(SessionNumbering.ARABIC).getValue(10))
        assertEquals("3", sessions.sessionNumberLabels(SessionNumbering.ARABIC).getValue(30))
    }
}

class LiveSessionNumberTest {
    @Test
    fun `the current session is the newest whatever its date says`() {
        val ended = LocalDateTime(2026, 9, 20, 22, 0)
        val sessions =
            listOf(
                note(id = 1, day = 5),
                note(id = 2, day = 20).copy(endedAt = ended),
                note(id = 3, day = 25).copy(endedAt = ended),
            )

        val index = sessions.chronologicalIndex()

        assertEquals(3, index.getValue(1))
        assertEquals(mapOf(2L to 3L, 3L to 1L), sessions.nextSessionIds())
    }
}

class NextSessionIdsTest {
    @Test
    fun `each session points at the one after it by date and the latest has none`() {
        val sessions = listOf(note(id = 20, day = 15), note(id = 10, day = 5), note(id = 30, day = 25))

        assertEquals(mapOf(10L to 20L, 20L to 30L), sessions.nextSessionIds())
    }
}
