package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import kotlin.test.Test
import kotlin.test.assertEquals

class PartyFormClassesTest {
    private val cleric = PartyClass("Жрец", "Домен Света", 3)
    private val fighter = PartyClass("Воин", "", 2)

    private fun fields() = CodexEntryFormFields(editingRef = null, type = EntityKind.PARTY)

    @Test
    fun `a form with one class lists it with the level field`() {
        val form = fields().copy(characterClass = "Жрец", subclass = "Домен Света", party = PartyFormFields(level = 4))
        assertEquals(listOf(PartyClass("Жрец", "Домен Света", 4)), form.classEntries())
    }

    @Test
    fun `a form with no class lists none`() {
        assertEquals(emptyList(), fields().classEntries())
    }

    @Test
    fun `two classes set the class fields and the level sum`() {
        val form = fields().withClassList(listOf(cleric, fighter))
        assertEquals("Жрец", form.characterClass)
        assertEquals("Домен Света", form.subclass)
        assertEquals(5, form.party.level)
        assertEquals(listOf(cleric, fighter), form.classEntries())
    }

    @Test
    fun `going back to one class clears the list and keeps its level`() {
        val form = fields().withClassList(listOf(cleric, fighter)).withClassList(listOf(fighter))
        assertEquals(emptyList(), form.party.classes)
        assertEquals("Воин", form.characterClass)
        assertEquals(2, form.party.level)
    }
}
