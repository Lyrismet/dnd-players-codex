package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartyClassesTest {
    private val cleric = PartyClass("Жрец", "Домен Света", 3)
    private val fighter = PartyClass("Воин", "", 2)

    private fun member(
        characterClass: String = "",
        subclass: String = "",
        level: Int? = null,
        own: Boolean = false,
    ) = PartyMember(
        id = 1,
        name = "Лира",
        characterClass = characterClass,
        race = "",
        level = level,
        playerName = "",
        isPlayerCharacter = own,
        presence = PartyPresence.IN,
        hpMax = null,
        hpCurrent = null,
        description = "",
        subclass = subclass,
    )

    @Test
    fun `classes survive the column text`() {
        val list = listOf(cleric, fighter)
        assertEquals(list, decodeClasses(list.encodeClasses()))
    }

    @Test
    fun `a broken line is skipped and an empty text is no classes`() {
        assertEquals(emptyList(), decodeClasses(""))
        assertEquals(listOf(fighter), decodeClasses("мусор\nВоин\t\t2"))
    }

    @Test
    fun `a tab or a new line typed in a name is replaced`() {
        val back = decodeClasses(listOf(PartyClass("Жрец\nX", "a\tb", 1)).encodeClasses())
        assertEquals(listOf(PartyClass("Жрец X", "a b", 1)), back)
    }

    @Test
    fun `the room for a level is what the other classes leave of twenty`() {
        val list = listOf(PartyClass("Жрец", "", 15), PartyClass("Воин", "", 4))
        assertEquals(1, list.levelRoom())
        assertEquals(16, list.levelRoom(index = 0))
        assertEquals(5, list.levelRoom(index = 1))
    }

    @Test
    fun `a class already listed is found in either language`() {
        val list = listOf(cleric)
        assertTrue(list.hasClass("жрец"))
        assertTrue(list.hasClass("Cleric"))
        assertFalse(list.hasClass("Воин"))
        assertFalse(list.hasClass("Жрец", exceptIndex = 0))
        assertFalse(list.hasClass(" "))
    }

    @Test
    fun `two classes set the summary from the first class and the level sum`() {
        val result = member(own = true).withClasses(listOf(cleric, fighter))
        assertEquals("Жрец", result.characterClass)
        assertEquals("Домен Света", result.subclass)
        assertEquals(5, result.level)
        assertEquals(listOf(cleric, fighter), result.classes)
    }

    @Test
    fun `one class goes back to the summary fields alone`() {
        val result = member(own = true).withClasses(listOf(cleric, fighter)).withClasses(listOf(fighter))
        assertEquals(emptyList(), result.classes)
        assertEquals("Воин", result.characterClass)
        assertEquals(2, result.level)
    }

    @Test
    fun `another member keeps its level and its classes carry none`() {
        val result = member("Воин", level = 7).withClasses(listOf(cleric, fighter))
        assertEquals(7, result.level)
        assertEquals("Жрец", result.characterClass)
        assertEquals(listOf(1, 1), result.classes.map { it.level })
        assertEquals("Жрец - Домен Света / Воин", result.classFact("{level} ур."))
        assertEquals(result, result.withClassSummary())
    }

    @Test
    fun `a single class of the summary is listed for the sheet`() {
        assertEquals(listOf(PartyClass("Воин", "Чемпион", 4)), member("Воин", "Чемпион", 4).classEntries())
        assertEquals(1, member("Воин").classEntries().single().level)
        assertEquals(emptyList(), member().classEntries())
    }

    @Test
    fun `a stored list from a file with one class or a level over the top is dropped`() {
        assertEquals("", normalizedClassesText(listOf(cleric).encodeClasses()))
        assertEquals(
            "",
            normalizedClassesText(listOf(PartyClass("a", "", 15), PartyClass("b", "", 15)).encodeClasses()),
        )
        assertEquals(
            listOf(cleric, fighter).encodeClasses(),
            normalizedClassesText(listOf(cleric, fighter).encodeClasses()),
        )
    }
}
