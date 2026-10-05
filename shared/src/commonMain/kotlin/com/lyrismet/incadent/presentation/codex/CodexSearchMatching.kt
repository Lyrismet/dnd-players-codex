package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.codexgroup.npcRegion
import com.lyrismet.incadent.core.codexgroup.questRegion
import com.lyrismet.incadent.core.codexgroup.resolvedRegion
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest

// the query arrives trimmed from the presenter, and an empty query matches every entry
private fun matchesAny(
    query: String,
    vararg fields: String?,
): Boolean = query.isEmpty() || fields.any { field -> field?.contains(query, ignoreCase = true) == true }

internal fun Npc.matchesQuery(
    query: String,
    locations: List<Location>,
): Boolean = matchesAny(query, name, race, faction, description, npcRegion(this, locations))

internal fun Quest.matchesQuery(
    query: String,
    npcs: List<Npc>,
    locations: List<Location>,
): Boolean =
    matchesAny(
        query,
        title,
        reward,
        description,
        npcs.firstOrNull { it.id == givenByNpcId }?.name,
        questRegion(this, npcs, locations),
    )

internal fun Location.matchesQuery(
    query: String,
    locations: List<Location>,
): Boolean = matchesAny(query, name, type, description, region, resolvedRegion(locations))

internal fun PartyMember.matchesQuery(query: String): Boolean =
    matchesAny(query, name, characterClass, race, playerName, description)
