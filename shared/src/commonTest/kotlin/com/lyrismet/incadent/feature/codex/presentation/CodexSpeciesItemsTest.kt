package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import kotlin.test.Test
import kotlin.test.assertEquals

private val labels = CreatureType.entries.associateWith { it.name.lowercase() }
private val goblin =
    Species(
        1,
        "Гоблин",
        CreatureType.HUMANOID,
        mapOf(DamageType.FIRE to DamageState.RESISTANCE, DamageType.COLD to DamageState.VULNERABILITY),
    )
private val skeleton = Species(2, "Скелет", CreatureType.UNDEAD)
private val leshy = Species(3, "Лёший", CreatureType.FEY)

class CodexSpeciesItemsTest {
    @Test
    fun `a row carries its type label shields and session count`() {
        val item = listOf(goblin).toCodexItems("", labels, mapOf(1L to 3)).single()
        assertEquals("humanoid", item.creatureTypeLabel)
        assertEquals(
            listOf(DamageType.COLD to DamageState.VULNERABILITY, DamageType.FIRE to DamageState.RESISTANCE),
            item.shields,
        )
        assertEquals(3, item.seenInSessions)
    }

    @Test
    fun `a species no note mentions has zero sessions`() {
        assertEquals(0, listOf(skeleton).toCodexItems("", labels, emptyMap()).single().seenInSessions)
    }

    @Test
    fun `the query finds a species by name and treats yo as ye`() {
        val all = listOf(goblin, skeleton, leshy)
        assertEquals(listOf(1L), all.toCodexItems("гобл", labels, emptyMap()).map { it.id })
        assertEquals(listOf(3L), all.toCodexItems("лёши", labels, emptyMap()).map { it.id })
        assertEquals(listOf(3L), all.toCodexItems("леши", labels, emptyMap()).map { it.id })
        assertEquals(emptyList(), all.toCodexItems("дракон", labels, emptyMap()))
    }
}
