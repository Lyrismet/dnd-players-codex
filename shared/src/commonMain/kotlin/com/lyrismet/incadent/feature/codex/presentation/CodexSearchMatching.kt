package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexRegions
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.Species

// the query arrives trimmed from the presenter, an empty query matches every entry, titles and npc nicknames are read
private inline fun matchesAnyTitle(
    query: String,
    titles: () -> List<String>,
): Boolean {
    if (query.isEmpty()) return true
    val folded = query.foldForSearch()
    return titles().any { it.foldForSearch().contains(folded) }
}

internal fun Species.matchesQuery(query: String): Boolean = matchesAnyTitle(query) { listOf(name) }

internal fun Npc.matchesQuery(
    query: String,
    regions: CodexRegions,
): Boolean = matchesAnyTitle(query) { listOf(name) + nicknames + regions.placeNamesOfNpc(this) }

internal fun Quest.matchesQuery(
    query: String,
    regions: CodexRegions,
): Boolean = matchesAnyTitle(query) { listOf(title) + regions.placeNamesOfQuest(this) }

internal fun Location.matchesQuery(
    query: String,
    regions: CodexRegions,
): Boolean = matchesAnyTitle(query) { listOf(name) + regions.placeNamesOfLocation(this) }

internal fun PartyMember.matchesQuery(query: String): Boolean = matchesAnyTitle(query) { listOf(name) }
