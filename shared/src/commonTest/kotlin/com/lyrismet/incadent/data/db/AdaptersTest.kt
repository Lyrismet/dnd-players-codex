package com.lyrismet.incadent.data.db

import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptersTest {
    @Test
    fun `a known enum value round-trips`() {
        assertEquals(QuestStatus.COMPLETED, questStatusAdapter.decode(questStatusAdapter.encode(QuestStatus.COMPLETED)))
    }

    @Test
    fun `an enum value this build does not know reads as the adapter's default`() {
        assertEquals(NpcStatus.NEUTRAL, npcStatusAdapter.decode("SOMETHING_FROM_A_NEWER_BUILD"))
        assertEquals(QuestStatus.ACTIVE, questStatusAdapter.decode(""))
    }

    @Test
    fun `a date time round-trips`() {
        val moment = LocalDateTime(2026, 3, 4, 18, 30)

        assertEquals(moment, localDateTimeAdapter.decode(localDateTimeAdapter.encode(moment)))
    }

    @Test
    fun `an unreadable date time reads as the epoch instead of crashing`() {
        assertEquals(LocalDateTime(1970, 1, 1, 0, 0), localDateTimeAdapter.decode("not a date"))
    }
}
