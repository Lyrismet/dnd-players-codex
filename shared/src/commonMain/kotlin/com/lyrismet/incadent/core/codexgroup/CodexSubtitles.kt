package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc

// the npc's default subtitle becomes "▲ place, place · faction/race" when the list is grouped by region
internal fun placeLine(
    npc: Npc,
    locations: List<Location>,
): String? {
    val names = npc.locationIds.mapNotNull { id -> locations.firstOrNull { it.id == id }?.name }
    if (names.isEmpty()) return null
    val factionOrRace = npc.faction.valueOrNull() ?: npc.race.valueOrNull().orEmpty()
    return joinWithDot("${MentionGlyph.LOCATION.symbol} ${names.joinToString()}", factionOrRace)
}
