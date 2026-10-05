package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc

// the npc's default subtitle becomes "▲ place · faction/race" when the list is grouped by region
internal fun placeLine(
    npc: Npc,
    locations: List<Location>,
): String? {
    val place = locations.byId(npc.locationId) ?: return null
    val factionOrRace = npc.faction.valueOrNull() ?: npc.race.valueOrNull().orEmpty()
    return joinWithDot("${MentionGlyph.LOCATION.symbol} ${place.name}", factionOrRace)
}

// a place inside a region names the region only when it differs from the group it is shown under
internal fun regionSubtitle(
    place: Location,
    locations: List<Location>,
): String {
    val ownRegion = place.region.valueOrNull()
    val groupRegion = place.resolvedRegion(locations)
    val outside = ownRegion?.takeIf { it != groupRegion }?.let { "в $it" }
    return joinWithDot(place.type, outside.orEmpty())
}
