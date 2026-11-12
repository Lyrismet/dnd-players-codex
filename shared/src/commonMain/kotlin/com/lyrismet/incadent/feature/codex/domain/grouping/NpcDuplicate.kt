package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Npc

/** the other npc that already has [name], or null when the name is free, letter case and edge spaces do not count */
fun duplicateNpcOf(
    name: String,
    npcs: List<Npc>,
    exceptId: Long?,
): Npc? {
    val typed = name.trim()
    if (typed.isEmpty()) return null
    return npcs.firstOrNull { it.id != exceptId && it.name.trim().equals(typed, ignoreCase = true) }
}
