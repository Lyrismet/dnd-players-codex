package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.Quest

/** region and place lookups over one snapshot of the lists - the section a row lands in and what a search can match */
class CodexRegions(
    private val npcs: List<Npc>,
    locations: List<Location>,
) {
    val places = PlaceTree(locations)

    /** one region per distinct place the npc can be met at, empty when it has no place */
    fun regionsOfNpc(npc: Npc): List<Location> =
        npc.locationIds
            .mapNotNull { places.find(it) }
            .map { places.regionOf(it) }
            .distinctBy { it.id }

    /** a quest's region comes from its "where" place, falling back to the first place of its giver */
    fun regionOfQuest(quest: Quest): Location? {
        val giver = npcs.firstOrNull { it.id == quest.givenByNpcId }
        val place = places.find(quest.locationId) ?: giver?.locationIds?.firstNotNullOfOrNull { places.find(it) }
        return place?.let { places.regionOf(it) }
    }

    fun regionOfLocation(place: Location): Location = places.regionOf(place)

    /** the names a search for a place can hit - every place the npc is met at and everything above them */
    fun placeNamesOfNpc(npc: Npc): List<String> =
        npc.locationIds.mapNotNull { places.find(it) }.flatMap { place -> places.pathOf(place).map { it.name } }

    fun placeNamesOfQuest(quest: Quest): List<String> {
        val giver = npcs.firstOrNull { it.id == quest.givenByNpcId }
        val place = places.find(quest.locationId)
        return if (place != null) places.pathOf(place).map { it.name } else giver?.let { placeNamesOfNpc(it) }.orEmpty()
    }

    fun placeNamesOfLocation(place: Location): List<String> = places.ancestorsOf(place).map { it.name }
}

internal fun List<Npc>.byId(id: Long?): Npc? = firstOrNull { it.id == id }
