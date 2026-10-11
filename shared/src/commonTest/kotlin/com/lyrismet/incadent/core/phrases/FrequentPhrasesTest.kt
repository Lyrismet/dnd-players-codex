package com.lyrismet.incadent.core.phrases

import com.lyrismet.incadent.domain.model.SessionEntry
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrequentPhrasesTest {
    private fun entry(
        id: Long,
        sessionId: Long,
        body: String,
    ) = SessionEntry(id, sessionId, LocalDateTime(2026, 1, 1, 12, 0), body)

    @Test
    fun `a chain repeated in two notes is found once with its counts`() {
        val result =
            findFrequentPhrases(
                listOf(
                    entry(1, 10, "Долгий отдых: хиты восстановлены."),
                    entry(2, 11, "Потом долгий отдых: хиты восстановлены, идём дальше"),
                ),
            )

        assertEquals(1, result.size)
        assertEquals(2, result.single().entryCount)
        assertEquals(2, result.single().sessionCount)
        assertEquals("Долгий отдых: хиты восстановлены", result.single().text)
    }

    @Test
    fun `a chain inside a longer found one is dropped`() {
        val result =
            findFrequentPhrases(
                listOf(
                    entry(1, 1, "Отряд пошёл к старой мельнице на рассвете"),
                    entry(2, 1, "Отряд пошёл к старой мельнице на рассвете снова"),
                ),
            )

        assertEquals(listOf("Отряд пошёл к старой мельнице на рассвете"), result.map { it.text })
    }

    @Test
    fun `a chain repeated inside one note only is not frequent`() {
        val result = findFrequentPhrases(listOf(entry(1, 1, "один два три четыре. один два три четыре.")))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `chains shorter than four words never count`() {
        val result = findFrequentPhrases(listOf(entry(1, 1, "раз два три"), entry(2, 1, "раз два три")))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `chains do not cross sentence ends`() {
        val result =
            findFrequentPhrases(listOf(entry(1, 1, "мы вошли. в город рано"), entry(2, 1, "мы вошли. в город рано")))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a chain is dropped as part of a longer one only along whole words`() {
        val result =
            findFrequentPhrases(
                listOf(
                    entry(1, 1, "навошёл в город рано утром"),
                    entry(2, 1, "навошёл в город рано утром"),
                    entry(3, 1, "вошёл в город рано"),
                    entry(4, 1, "вошёл в город рано"),
                ),
            )

        assertTrue("вошёл в город рано" in result.map { it.text })
    }

    @Test
    fun `a shorter chain found in more notes stays next to the longer one`() {
        val result =
            findFrequentPhrases(
                listOf(
                    entry(1, 1, "мы шли по тропе к реке"),
                    entry(2, 1, "мы шли по тропе к реке"),
                    entry(3, 1, "мы шли по тропе"),
                    entry(4, 1, "мы шли по тропе"),
                ),
            )

        assertEquals(listOf("мы шли по тропе", "мы шли по тропе к реке"), result.map { it.text })
    }
}
