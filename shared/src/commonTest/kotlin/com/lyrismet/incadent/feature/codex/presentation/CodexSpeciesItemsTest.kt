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
        nicknames = listOf("Зелёный"),
    )
private val skeleton = Species(2, "Скелет", CreatureType.UNDEAD)

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
    fun `the query finds a species by name or nickname and treats yo as ye`() {
        val all = listOf(goblin, skeleton)
        assertEquals(listOf(1L), all.toCodexItems("гобл", labels, emptyMap()).map { it.id })
        assertEquals(listOf(1L), all.toCodexItems("зелёный", labels, emptyMap()).map { it.id })
        assertEquals(listOf(1L), all.toCodexItems("зеленый", labels, emptyMap()).map { it.id })
        assertEquals(emptyList(), all.toCodexItems("дракон", labels, emptyMap()))
    }
}
