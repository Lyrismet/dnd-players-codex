package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc

// the same glyph a place mention chip carries
private const val PLACE_GLYPH = "▲"

// the npc's default subtitle becomes "▲ place, place · faction/race" when the list is grouped by region
internal fun placeLine(
    npc: Npc,
    locations: List<Location>,
): String? {
    val names = npc.locationIds.mapNotNull { id -> locations.firstOrNull { it.id == id }?.name }
    if (names.isEmpty()) return null
    val factionOrRace = npc.faction.valueOrNull() ?: npc.race.valueOrNull().orEmpty()
    return joinWithDot("$PLACE_GLYPH ${names.joinToString()}", factionOrRace)
}
