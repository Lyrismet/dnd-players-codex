package com.lyrismet.incadent.feature.phrases.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhraseSnippetsTest {
    private val pool =
        listOf(
            PhraseOption("Долгий отдых: хиты восстановлены", isPinned = true),
            PhraseOption("Долгий путь по тропе", isPinned = false, entryCount = 3),
        )

    @Test
    fun `pool lists pinned first and hides frequent phrases that are already pinned`() {
        val result =
            phrasePool(
                pinned = listOf("Моя фраза"),
                frequent = listOf(FrequentPhrase("Моя фраза", 2, 1), FrequentPhrase("Другая фраза тут", 4, 2)),
            )

        assertEquals(listOf("Моя фраза", "Другая фраза тут"), result.map { it.text })
        assertEquals(listOf(true, false), result.map { it.isPinned })
    }

    @Test
    fun `completions match the sentence being typed ignoring case`() {
        val result = phraseCompletions("Пришли в город. долгий", pool)

        assertEquals("долгий", result?.fragment)
        assertEquals(2, result?.options?.size)
    }

    @Test
    fun `completions need three typed characters and a longer phrase`() {
        assertNull(phraseCompletions("До", pool))
        assertNull(phraseCompletions("Долгий путь по тропе", pool))
        assertNull(phraseCompletions("   ", pool))
    }

    @Test
    fun `inserting appends with one space or replaces the typed fragment`() {
        assertEquals("Привет Фраза ", insertPhrase("Привет", "Фраза"))
        assertEquals("Привет Фраза ", insertPhrase("Привет ", "Фраза"))
        assertEquals("Фраза ", insertPhrase("", "Фраза"))
        assertEquals("Пришли. Долгий путь ", insertPhrase("Пришли. дол", "Долгий путь", replacing = "дол"))
    }

    @Test
    fun `label drops mention markers and cuts long text`() {
        assertEquals("Бран скрылся", phraseLabel("@Бран скрылся"))
        assertEquals("Взять Ключ", phraseLabel("Взять @Ключ"))
        assertEquals("Длинная…", phraseLabel("Длинная фраза", maxLength = 8))
    }

    @Test
    fun `snippet from a note is one line and cut at a word boundary`() {
        assertEquals("Первая строка вторая строка", snippetFromEntry("  Первая строка\n\nвторая   строка "))

        val long = "слово ".repeat(60)
        val result = snippetFromEntry(long)
        assertEquals(true, result.length <= 200 && result.endsWith("слово"))
    }
}
