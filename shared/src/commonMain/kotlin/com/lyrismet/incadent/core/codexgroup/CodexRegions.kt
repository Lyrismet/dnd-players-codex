package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.Quest

/** region lookups over one snapshot of the lists - each region is resolved at most once, and only when asked for */
class CodexRegions(
    private val npcs: List<Npc>,
    quests: List<Quest>,
    private val locations: List<Location>,
) {
    private val npcRegions = npcs.associate { npc -> npc.id to lazy { npcRegion(npc, locations) } }
    private val questRegions = quests.associate { quest -> quest.id to lazy { questRegion(quest, npcs, locations) } }
    private val locationRegions = locations.associate { place -> place.id to lazy { place.resolvedRegion(locations) } }

    fun regionOfNpc(npc: Npc): String? = npcRegions.resolve(npc.id) { npcRegion(npc, locations) }

    fun regionOfQuest(quest: Quest): String? = questRegions.resolve(quest.id) { questRegion(quest, npcs, locations) }

    fun regionOfLocation(place: Location): String? =
        locationRegions.resolve(place.id) { place.resolvedRegion(locations) }
}

// an entity outside the snapshot is resolved directly, and a cached null region is still a hit
private fun <T> Map<Long, Lazy<T>>.resolve(
    id: Long,
    direct: () -> T,
): T {
    val cached = get(id) ?: return direct()
    return cached.value
}
