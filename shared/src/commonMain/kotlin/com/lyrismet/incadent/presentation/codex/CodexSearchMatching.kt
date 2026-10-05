package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest

internal fun Npc.matchesQuery(query: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true) || description.contains(query, ignoreCase = true)

internal fun Quest.matchesQuery(
    query: String,
    npcsById: Map<Long, Npc>,
): Boolean {
    if (query.isEmpty()) return true
    val giverName = npcsById[givenByNpcId]?.name.orEmpty()
    return title.contains(query, ignoreCase = true) ||
        reward.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        giverName.contains(query, ignoreCase = true)
}

internal fun Location.matchesQuery(query: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true) || type.contains(query, ignoreCase = true)

internal fun PartyMember.matchesQuery(query: String): Boolean =
    query.isEmpty() ||
        name.contains(query, ignoreCase = true) ||
        characterClass.contains(query, ignoreCase = true) ||
        playerName.contains(query, ignoreCase = true)
