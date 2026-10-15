package com.lyrismet.incadent.core.format

import com.lyrismet.incadent.core.preferences.AppLanguage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

private val RUSSIAN_GENITIVE_MONTHS =
    listOf(
        "января",
        "февраля",
        "марта",
        "апреля",
        "мая",
        "июня",
        "июля",
        "августа",
        "сентября",
        "октября",
        "ноября",
        "декабря",
    )

private val ENGLISH_MONTHS =
    listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
    )

private val RUSSIAN_SHORT_MONTHS_UPPER =
    listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЯ", "ИЮН", "ИЮЛ", "АВГ", "СЕНТ", "ОКТ", "НОЯБ", "ДЕК")

private const val ENGLISH_SHORT_LENGTH = 3

private fun Month.index(): Int = ordinal

/** "26 сентября 2026" or "September 26, 2026" - the date format used throughout the session diary */
fun LocalDateTime.toDisplayDate(language: AppLanguage): String =
    when (language) {
        AppLanguage.RUSSIAN -> "$day ${RUSSIAN_GENITIVE_MONTHS[month.index()]} $year"
        AppLanguage.ENGLISH -> "${ENGLISH_MONTHS[month.index()]} $day, $year"
    }

/** "09:41" - the timestamp shown next to each entry in a session's feed */
fun LocalDateTime.toDisplayTime(): String {
    val hour = this.hour.toString().padStart(2, '0')
    val minute = this.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

/** "19 СЕНТ" or "SEP 19" - the day-separator label between a multi-day session's meetings */
fun LocalDate.toShortDayMonthUpper(language: AppLanguage): String =
    when (language) {
        AppLanguage.RUSSIAN -> "$day ${RUSSIAN_SHORT_MONTHS_UPPER[month.index()]}"
        AppLanguage.ENGLISH -> "${ENGLISH_MONTHS[month.index()].take(ENGLISH_SHORT_LENGTH).uppercase()} $day"
    }

/** "12 июня" or "June 12" - the date without the year, for a compact meta label */
fun LocalDateTime.toShortDisplayDate(language: AppLanguage): String =
    when (language) {
        AppLanguage.RUSSIAN -> "$day ${RUSSIAN_GENITIVE_MONTHS[month.index()]}"
        AppLanguage.ENGLISH -> "${ENGLISH_MONTHS[month.index()]} $day"
    }
