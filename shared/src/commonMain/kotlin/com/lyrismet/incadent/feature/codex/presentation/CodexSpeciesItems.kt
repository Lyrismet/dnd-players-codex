package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Species

/** the species whose name or nickname holds [query] as rows, [seenBySpecies] counts the sessions that mention each */
internal fun List<Species>.toCodexItems(
    query: String,
    creatureTypeLabels: Map<CreatureType, String>,
    seenBySpecies: Map<Long, Int>,
    fightsBySpecies: Map<Long, Int> = emptyMap(),
): List<SpeciesCodexItem> =
    filter { it.matchesQuery(query) }.map { species ->
        SpeciesCodexItem(
            id = species.id,
            name = species.name,
            creatureTypeLabel = creatureTypeLabels.getValue(species.creatureType),
            photo = species.photo,
            shields = species.vulnerabilities.shields(),
            seenInSessions = seenBySpecies[species.id] ?: 0,
            fights = fightsBySpecies[species.id] ?: 0,
        )
    }
