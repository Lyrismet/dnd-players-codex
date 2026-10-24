package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickEditSpeciesTest {
    private val species = Species(id = 1, name = "Гоблин", creatureType = CreatureType.HUMANOID)

    @Test
    fun `a name edit is trimmed and capitalized`() {
        val edited = species.withQuickEdit(QuickEditField.NAME, QuickEditValue.Text("  волк "))

        assertEquals("Волк", edited?.name)
        assertEquals(CreatureType.HUMANOID, edited?.creatureType)
    }

    @Test
    fun `a description edit writes the notes`() {
        assertEquals(
            "Любит золото",
            species.withQuickEdit(QuickEditField.DESCRIPTION, QuickEditValue.Text("Любит золото"))?.notes,
        )
    }

    @Test
    fun `a nicknames edit changes nothing for a species`() {
        assertEquals(null, species.withQuickEdit(QuickEditField.NICKNAMES, QuickEditValue.Text("Гоб")))
    }

    @Test
    fun `a creature type edit switches the type`() {
        val edited = species.withQuickEdit(QuickEditField.CREATURE_TYPE, QuickEditValue.Creature(CreatureType.BEAST))

        assertEquals(CreatureType.BEAST, edited?.creatureType)
    }

    @Test
    fun `hit points are set and cleared`() {
        val set = species.withQuickEdit(QuickEditField.HIT_POINTS, QuickEditValue.Number(30))

        assertEquals(30, set?.hitPoints)
        assertNull(set?.withQuickEdit(QuickEditField.HIT_POINTS, QuickEditValue.Number(null))?.hitPoints)
    }

    @Test
    fun `a field a species does not have writes nothing`() {
        assertNull(species.withQuickEdit(QuickEditField.REWARD, QuickEditValue.Text("x")))
    }
}
