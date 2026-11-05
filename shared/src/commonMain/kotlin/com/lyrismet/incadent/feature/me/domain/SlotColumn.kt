package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.RESOURCE_MAX_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.ResourceTemplate
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS

/** one column of the slot grid, only a level with at least one slot gets one */
data class SlotColumn(
    val level: Int,
    val resource: CharacterResource,
)

/** the columns of levels 1-9 in order, a level without slots is left out */
fun slotColumns(resources: List<CharacterResource>): List<SlotColumn> =
    resources
        .mapNotNull { resource -> resource.slotLevel?.takeIf { resource.max > 0 }?.let { SlotColumn(it, resource) } }
        .sortedBy { it.level }

/** the resources that are plain counters, in the order the player keeps them */
fun featureResources(resources: List<CharacterResource>): List<CharacterResource> =
    resources.filter { !it.isSpellSlot }.sortedWith(compareBy({ it.position }, { it.id }))

/** the left count after a press on the cell at [index], a lit cell is spent and a dark one comes back */
fun cellPressed(
    current: Int,
    index: Int,
): Int = if (index < current) current - 1 else current + 1

/** the maxima per level to start the slot sheet from, 0 for a level the character has no slots of */
fun slotMaxima(resources: List<CharacterResource>): Map<Int, Int> {
    val byLevel = resources.mapNotNull { resource -> resource.slotLevel?.let { it to resource.max } }.toMap()
    return SPELL_LEVELS.associateWith { byLevel[it] ?: 0 }
}

/** the next position for a new resource, after the last one */
fun nextPosition(resources: List<CharacterResource>): Int = (resources.maxOfOrNull { it.position } ?: -1) + 1

/** the templates worth offering, the slots one goes once the character has slots */
fun availableTemplates(resources: List<CharacterResource>): List<ResourceTemplate> {
    val hasSlots = resources.any { it.isSpellSlot }
    return ResourceTemplate.entries.filter { !(hasSlots && it == ResourceTemplate.SPELL_SLOTS) }
}

fun Int.steppedMax(delta: Int): Int = (this + delta).coerceIn(RESOURCE_MAX_RANGE)
