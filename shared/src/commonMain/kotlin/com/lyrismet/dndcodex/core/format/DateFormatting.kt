package com.lyrismet.dndcodex.core.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

private val RUSSIAN_GENITIVE_MONTHS =
    mapOf(
        Month.JANUARY to "января",
        Month.FEBRUARY to "февраля",
        Month.MARCH to "марта",
        Month.APRIL to "апреля",
        Month.MAY to "мая",
        Month.JUNE to "июня",
        Month.JULY to "июля",
        Month.AUGUST to "августа",
        Month.SEPTEMBER to "сентября",
        Month.OCTOBER to "октября",
        Month.NOVEMBER to "ноября",
        Month.DECEMBER to "декабря",
    )

private val RUSSIAN_SHORT_MONTHS_UPPER =
    mapOf(
        Month.JANUARY to "ЯНВ",
        Month.FEBRUARY to "ФЕВ",
        Month.MARCH to "МАР",
        Month.APRIL to "АПР",
        Month.MAY to "МАЯ",
        Month.JUNE to "ИЮН",
        Month.JULY to "ИЮЛ",
        Month.AUGUST to "АВГ",
        Month.SEPTEMBER to "СЕНТ",
        Month.OCTOBER to "ОКТ",
        Month.NOVEMBER to "НОЯБ",
        Month.DECEMBER to "ДЕК",
    )

/** "26 сентября 2026" - the date format used throughout the session diary */
fun LocalDateTime.toDisplayDate(): String {
    val month = RUSSIAN_GENITIVE_MONTHS.getValue(this.month)
    return "${this.day} $month ${this.year}"
}

/** "09:41" - the timestamp shown next to each entry in a session's feed */
fun LocalDateTime.toDisplayTime(): String {
    val hour = this.hour.toString().padStart(2, '0')
    val minute = this.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

/** "19 СЕНТ" - the day-separator label between a multi-day session's meetings */
fun LocalDate.toShortDayMonthUpper(): String = "$day ${RUSSIAN_SHORT_MONTHS_UPPER.getValue(month)}"
