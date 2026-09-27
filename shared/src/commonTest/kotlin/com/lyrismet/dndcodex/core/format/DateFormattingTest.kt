package com.lyrismet.dndcodex.core.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormattingTest {
    @Test
    fun `toDisplayDate renders the day and genitive month and year`() {
        assertEquals("26 сентября 2026", LocalDateTime(2026, 9, 26, 10, 0).toDisplayDate())
        assertEquals("1 января 2027", LocalDateTime(2027, 1, 1, 0, 0).toDisplayDate())
    }

    @Test
    fun `toDisplayTime pads hours and minutes to two digits`() {
        assertEquals("09:05", LocalDateTime(2026, 9, 26, 9, 5).toDisplayTime())
        assertEquals("23:59", LocalDateTime(2026, 9, 26, 23, 59).toDisplayTime())
    }

    @Test
    fun `toShortDayMonthUpper renders the day and short uppercase month`() {
        assertEquals("19 СЕНТ", LocalDate(2026, 9, 19).toShortDayMonthUpper())
        assertEquals("1 МАЯ", LocalDate(2026, 5, 1).toShortDayMonthUpper())
    }
}
