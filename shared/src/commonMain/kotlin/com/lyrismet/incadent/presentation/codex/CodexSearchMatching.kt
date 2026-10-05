package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.codexgroup.CodexRegions
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest

// the query arrives trimmed from the presenter, and an empty query matches every entry without reading any field
private inline fun matchesAny(
    query: String,
    fields: () -> List<String?>,
): Boolean = query.isEmpty() || fields().any { field -> field?.contains(query, ignoreCase = true) == true }

internal fun Npc.matchesQuery(
    query: String,
    regions: CodexRegions,
): Boolean = matchesAny(query) { listOf(name, race, faction, description, regions.regionOfNpc(this)) }

internal fun Quest.matchesQuery(
    query: String,
    npcsById: Map<Long, Npc>,
    regions: CodexRegions,
): Boolean =
    matchesAny(query) {
        listOf(title, reward, description, npcsById[givenByNpcId]?.name, regions.regionOfQuest(this))
    }

internal fun Location.matchesQuery(
    query: String,
    regions: CodexRegions,
): Boolean = matchesAny(query) { listOf(name, type, description, region, regions.regionOfLocation(this)) }

internal fun PartyMember.matchesQuery(query: String): Boolean =
    matchesAny(query) { listOf(name, characterClass, race, playerName, description) }
