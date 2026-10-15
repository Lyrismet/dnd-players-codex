package com.lyrismet.incadent.feature.session.domain.tags

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionTagSuggestionsTest {
    @Test
    fun `empty session suggests nothing`() {
        assertEquals(emptyList(), suggestSessionTags(emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun `a prefix stem matches a word that extends it`() {
        val result = suggestSessionTags(listOf("Отряд попал в засаду у моста"), emptyList(), emptyList())
        val combat = result.single { it.tag == "Бой" }
        assertTrue("засаду" in combat.reason)
    }

    @Test
    fun `a prefix stem does not match mid-word without a boundary`() {
        val result = suggestSessionTags(listOf("Нашли обойму патронов в тайнике"), emptyList(), emptyList())
        assertTrue(result.none { it.tag == "Бой" })
    }

    @Test
    fun `the exact stem matches the standalone word`() {
        val result = suggestSessionTags(listOf("Заплатили 200 зм за информацию"), emptyList(), emptyList())
        assertTrue(result.any { it.tag == "Торговля" })
    }

    @Test
    fun `the exact stem does not match inside a longer word`() {
        val result = suggestSessionTags(listOf("На столе лежала призма из кварца"), emptyList(), emptyList())
        assertTrue(result.none { it.tag == "Торговля" })
    }

    @Test
    fun `a quest mention is the only signal for syuzhet`() {
        val result =
            suggestSessionTags(listOf("Поговорили со старостой о делах"), listOf("Ключ от склепа"), emptyList())
        val plot = result.single { it.tag == "Сюжет" }
        assertEquals("Квест: Ключ от склепа", plot.reason)
    }

    @Test
    fun `two or more quests pluralize the reason and cap at two names`() {
        val result =
            suggestSessionTags(
                emptyList(),
                listOf("Ключ от склепа", "Пропавший караван", "Тени Блэквуда"),
                emptyList(),
            )
        val plot = result.single { it.tag == "Сюжет" }
        assertEquals("Квесты: Ключ от склепа, Пропавший караван", plot.reason)
    }

    @Test
    fun `an already assigned tag is never suggested again`() {
        val result = suggestSessionTags(listOf("Отряд напал первым"), emptyList(), listOf("Бой"))
        assertTrue(result.none { it.tag == "Бой" })
    }

    @Test
    fun `the reason lists at most two distinct matched words`() {
        val result =
            suggestSessionTags(
                listOf("Нашли улики, допросили свидетеля и разгадали загадку про следы"),
                emptyList(),
                emptyList(),
            )
        val investigation = result.single { it.tag == "Расследование" }
        assertEquals(2, investigation.reason.count { it == '«' })
    }

    @Test
    fun `matching is case-insensitive`() {
        val result = suggestSessionTags(listOf("ОТРЯД УШЁЛ НА ОТДЫХ В ЛАГЕРЕ"), emptyList(), emptyList())
        assertTrue(result.any { it.tag == "Отдых" })
    }

    @Test
    fun `reason lines use the labels the caller passes`() {
        val labels = TagReasonLabels(quest = "Quest: ", quests = "Quests: ", words = "Words in notes: ")
        val words = suggestSessionTags(listOf("Отряд попал в засаду"), emptyList(), emptyList(), labels)
        assertTrue(words.single { it.tag == "Бой" }.reason.startsWith("Words in notes: "))
        val quests = suggestSessionTags(emptyList(), listOf("Меч", "Щит"), emptyList(), labels)
        assertTrue(quests.single { it.tag == "Сюжет" }.reason.startsWith("Quests: "))
    }

    @Test
    fun `an english word suggests the same tag as its russian one`() {
        val result = suggestSessionTags(listOf("The party fell into an ambush"), emptyList(), emptyList())
        assertTrue(result.any { it.tag == "Бой" })
    }

    @Test
    fun `an english whole word stem does not match inside a longer word`() {
        val noRest = suggestSessionTags(listOf("The campaign went on in the restaurant"), emptyList(), emptyList())
        assertTrue(noRest.none { it.tag == "Отдых" })
        val rest = suggestSessionTags(listOf("Time to rest"), emptyList(), emptyList())
        assertTrue(rest.any { it.tag == "Отдых" })
    }
}
