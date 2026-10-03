package com.lyrismet.incadent.core.format

import com.lyrismet.incadent.domain.model.SessionNote
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
}
