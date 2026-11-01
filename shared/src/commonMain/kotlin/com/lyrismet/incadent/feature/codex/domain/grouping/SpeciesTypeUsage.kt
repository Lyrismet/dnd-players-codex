package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Species

/** the names of the species that carry the player's own type [typeId], what a delete of that type would touch */
fun List<Species>.namesWithCustomType(typeId: Long): List<String> =
    filter { it.customType?.id == typeId }.map { it.name }.sorted()
