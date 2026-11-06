package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.minutesSince
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveFightMinutesTest {
    private val start = LocalDateTime(2026, 11, 6, 19, 0)

    @Test
    fun `whole minutes are counted across the hour`() {
        assertEquals(25, minutesSince(start, LocalDateTime(2026, 11, 6, 19, 25, 59)))
        assertEquals(75, minutesSince(start, LocalDateTime(2026, 11, 6, 20, 15)))
    }

    @Test
    fun `a clock that went back reads as zero`() {
        assertEquals(0, minutesSince(start, LocalDateTime(2026, 11, 6, 18, 0)))
    }
}
