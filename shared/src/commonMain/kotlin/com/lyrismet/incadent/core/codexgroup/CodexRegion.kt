package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.Quest

// caps the place-to-region chain so a region loop still terminates
private const val MAX_REGION_DEPTH = 5

/** the region of a place, following places named as regions - null when the place has no region */
fun Location.resolvedRegion(locations: List<Location>): String? = resolvedRegion(this, locations, depth = 0)

/** an npc's region is the region of the place it is met at - null when it has no place */
fun npcRegion(
    npc: Npc,
    locations: List<Location>,
): String? = locations.byId(npc.locationId)?.resolvedRegion(locations)

/** a quest's region comes from its "where" place, falling back to the place of its giver */
fun questRegion(
    quest: Quest,
    npcs: List<Npc>,
    locations: List<Location>,
): String? {
    val giverPlaceId = npcs.byId(quest.givenByNpcId)?.locationId
    val place = locations.byId(quest.locationId) ?: locations.byId(giverPlaceId)
    return place?.resolvedRegion(locations)
}

private fun resolvedRegion(
    place: Location,
    locations: List<Location>,
    depth: Int,
): String? {
    val region = place.region.valueOrNull() ?: return null
    val regionPlace = locations.firstOrNull { it.name == region && it.id != place.id }
    return if (regionPlace != null && depth < MAX_REGION_DEPTH) {
        resolvedRegion(regionPlace, locations, depth + 1) ?: region
    } else {
        region
    }
}

internal fun List<Location>.byId(id: Long?): Location? = firstOrNull { it.id == id }

internal fun List<Npc>.byId(id: Long?): Npc? = firstOrNull { it.id == id }
