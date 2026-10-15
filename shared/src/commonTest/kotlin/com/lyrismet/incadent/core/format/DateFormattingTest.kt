package com.lyrismet.incadent.core.format

import com.lyrismet.incadent.core.preferences.AppLanguage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormattingTest {
    @Test
    fun `toDisplayDate renders the day and genitive month and year in Russian`() {
        assertEquals("26 сентября 2026", LocalDateTime(2026, 9, 26, 10, 0).toDisplayDate(AppLanguage.RUSSIAN))
        assertEquals("1 января 2027", LocalDateTime(2027, 1, 1, 0, 0).toDisplayDate(AppLanguage.RUSSIAN))
    }

    @Test
    fun `toDisplayDate renders the month name first in English`() {
        assertEquals("September 26, 2026", LocalDateTime(2026, 9, 26, 10, 0).toDisplayDate(AppLanguage.ENGLISH))
    }

    @Test
    fun `toShortDisplayDate drops the year`() {
        assertEquals("12 июня", LocalDateTime(2026, 6, 12, 0, 0).toShortDisplayDate(AppLanguage.RUSSIAN))
        assertEquals("June 12", LocalDateTime(2026, 6, 12, 0, 0).toShortDisplayDate(AppLanguage.ENGLISH))
    }

    @Test
    fun `toDisplayTime pads hours and minutes to two digits`() {
        assertEquals("09:05", LocalDateTime(2026, 9, 26, 9, 5).toDisplayTime())
        assertEquals("23:59", LocalDateTime(2026, 9, 26, 23, 59).toDisplayTime())
    }

    @Test
    fun `toShortDayMonthUpper renders the day and short uppercase month`() {
        assertEquals("19 СЕНТ", LocalDate(2026, 9, 19).toShortDayMonthUpper(AppLanguage.RUSSIAN))
        assertEquals("1 МАЯ", LocalDate(2026, 5, 1).toShortDayMonthUpper(AppLanguage.RUSSIAN))
        assertEquals("SEP 19", LocalDate(2026, 9, 19).toShortDayMonthUpper(AppLanguage.ENGLISH))
    }
}
