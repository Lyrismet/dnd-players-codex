package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.Npc

private const val MAX_EXISTING = 3

/** what a name typed over a participant can mean: a new npc, a known one, or a caption that stays in this fight */
sealed interface NameChoice {
    data class New(
        val name: String,
    ) : NameChoice

    data class Existing(
        val npcId: Long,
        val name: String,
    ) : NameChoice

    data class Plain(
        val name: String,
    ) : NameChoice
}

/** the options for [query]: a new npc unless the name is taken, up to three known ones, a caption last */
fun nameChoices(
    query: String,
    speciesId: Long?,
    npcs: List<Npc>,
): List<NameChoice> {
    val name = query.trim()
    if (name.isEmpty()) return emptyList()
    val matches =
        npcs
            .filter { it.name.contains(name, ignoreCase = true) }
            .sortedByDescending { it.speciesId != null && it.speciesId == speciesId }
            .take(MAX_EXISTING)
    val exact = npcs.any { it.name.equals(name, ignoreCase = true) }
    return buildList {
        if (!exact) add(NameChoice.New(name))
        matches.forEach { add(NameChoice.Existing(it.id, it.name)) }
        add(NameChoice.Plain(name))
    }
}
