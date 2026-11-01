package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.summary.SpeciesTypeLabels
import com.lyrismet.incadent.feature.combat.domain.results.SpeciesCombatFacts

/** the species whose name holds [query] as rows, [seenBySpecies] counts the sessions that mention each */
internal fun List<Species>.toCodexItems(
    query: String,
    typeLabels: SpeciesTypeLabels,
    seenBySpecies: Map<Long, Int>,
    factsBySpecies: Map<Long, SpeciesCombatFacts> = emptyMap(),
): List<SpeciesCodexItem> =
    filter { it.matchesQuery(query) }.map { species ->
        SpeciesCodexItem(
            id = species.id,
            name = species.name,
            creatureTypeLabel = typeLabels.of(species),
            photo = species.photo,
            shields = species.vulnerabilities.shields(),
            seenInSessions = seenBySpecies[species.id] ?: 0,
            fights = factsBySpecies[species.id]?.fights ?: 0,
            hitPoints = species.hitPoints,
            hitRange = factsBySpecies[species.id]?.hitRange,
        )
    }
