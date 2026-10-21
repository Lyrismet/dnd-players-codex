package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species

// the same glyph a place mention chip carries
private const val PLACE_GLYPH = "▲"

// the npc's list subtitle is its species, grouped by region it is "▲ place, place · species or race" instead
internal fun npcSubtitle(
    npc: Npc,
    locations: List<Location>,
    species: List<Species>,
    withPlaces: Boolean,
): String? {
    val speciesName = species.firstOrNull { it.id == npc.speciesId }?.name
    if (!withPlaces) return speciesName
    val names = npc.locationIds.mapNotNull { id -> locations.firstOrNull { it.id == id }?.name }
    val places = if (names.isEmpty()) "" else "$PLACE_GLYPH ${names.joinToString()}"
    val kind = speciesName ?: npc.race.valueOrNull().orEmpty()
    return joinWithDot(places, kind).takeIf { it.isNotBlank() }
}
