package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.CustomCreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeciesTypeUsageTest {
    private val machine = CustomCreatureType(1, "механизм")
    private val golem = Species(1, "Голем", CreatureType.UNSPECIFIED).withType(SpeciesType.Custom(machine))
    private val clock = Species(2, "Часовщик", CreatureType.UNSPECIFIED).withType(SpeciesType.Custom(machine))
    private val wolf = Species(3, "Волк", CreatureType.BEAST)

    @Test
    fun `the species of an own type are listed by name`() {
        assertEquals(listOf("Голем", "Часовщик"), listOf(wolf, clock, golem).namesWithCustomType(machine.id))
    }

    @Test
    fun `a type nobody has lists nothing`() {
        assertEquals(emptyList(), listOf(wolf, golem).namesWithCustomType(99))
    }

    @Test
    fun `a species reads as built-in own or none`() {
        assertEquals(SpeciesType.Builtin(CreatureType.BEAST), wolf.type)
        assertEquals(SpeciesType.Custom(machine), golem.type)
        assertEquals(SpeciesType.None, golem.withType(SpeciesType.None).type)
    }

    @Test
    fun `switching to a built-in type drops the own one`() {
        val switched = golem.withType(SpeciesType.Builtin(CreatureType.CONSTRUCT))

        assertEquals(CreatureType.CONSTRUCT, switched.creatureType)
        assertEquals(null, switched.customType)
    }
}
