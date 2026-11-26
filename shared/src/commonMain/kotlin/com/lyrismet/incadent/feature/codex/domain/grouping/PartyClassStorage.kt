package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges

private const val ENTRY_SEPARATOR = '\n'
private const val FIELD_SEPARATOR = '\t'
private const val LEVEL_FIELD = 2
private const val FIELD_COUNT = 3

/** the classes as the text a column stores, one class per line with its three fields split by a tab */
fun List<PartyClass>.encodeClasses(): String =
    joinToString(ENTRY_SEPARATOR.toString()) {
        listOf(
            it.characterClass.clean(),
            it.subclass.clean(),
            it.level.toString(),
        ).joinToString(FIELD_SEPARATOR.toString())
    }

/** reads what [encodeClasses] wrote, a line this build cannot read is skipped instead of failing the row */
fun decodeClasses(raw: String): List<PartyClass> =
    if (raw.isEmpty()) {
        emptyList()
    } else {
        raw.split(ENTRY_SEPARATOR).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR)
            val level = parts.getOrNull(LEVEL_FIELD)?.toIntOrNull() ?: return@mapNotNull null
            if (parts.size != FIELD_COUNT) return@mapNotNull null
            PartyClass(parts[0], parts[1], level)
        }
    }

/** a stored list read from a file, kept only when it still holds two classes or more and each level is in range */
fun normalizedClassesText(raw: String): String =
    decodeClasses(raw)
        .map { it.copy(level = it.level.coerceIn(PartyStatRanges.level)) }
        .takeIf { it.size >= 2 && it.totalLevel() <= PartyStatRanges.level.last }
        .orEmpty()
        .encodeClasses()

private fun String.clean() = replace(ENTRY_SEPARATOR, ' ').replace(FIELD_SEPARATOR, ' ')
