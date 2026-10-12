package com.lyrismet.incadent.core.selectlist

import com.lyrismet.incadent.core.codexgroup.collationKey

/** one entry of a select list - [searchText] is what a query is matched against, usually the title plus the subtitle */
data class SelectOption(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val searchText: String = title,
)

/** a shown option, [match] is the part of the title the query hit - null when the hit is only in the subtitle */
data class SelectRow(
    val option: SelectOption,
    val selected: Boolean,
    val match: IntRange? = null,
)

/**
 * the rows for a query: filtered by [SelectOption.searchText], minus [excludedIds]. Order is the checked ones first
 * (only when [floatSelected] and nothing is typed), then recently used ones in [recentIds] order, then by title.
 */
@Suppress("LongParameterList")
fun selectRows(
    options: List<SelectOption>,
    query: String,
    selectedIds: Set<Long>,
    recentIds: List<Long> = emptyList(),
    excludedIds: Set<Long> = emptySet(),
    floatSelected: Boolean = false,
): List<SelectRow> {
    val needle = query.trim()
    val recentRank = recentIds.withIndex().associate { (index, id) -> id to index }
    return options
        .asSequence()
        .filter { it.id !in excludedIds }
        .filter { needle.isEmpty() || it.searchText.contains(needle, ignoreCase = true) }
        .sortedWith(
            compareBy<SelectOption>(
                { if (floatSelected && needle.isEmpty() && it.id in selectedIds) 0 else 1 },
                { recentRank[it.id] ?: Int.MAX_VALUE },
                { it.title.collationKey() },
                { it.id },
            ),
        ).map { option -> SelectRow(option, option.id in selectedIds, option.title.matchRange(needle)) }
        .toList()
}

/** a "create «query»" row is offered when something is typed and no option already has exactly that title */
fun canCreateFromQuery(
    options: List<SelectOption>,
    query: String,
): Boolean {
    val needle = query.trim()
    return needle.isNotEmpty() && options.none { it.title.equals(needle, ignoreCase = true) }
}

private fun String.matchRange(needle: String): IntRange? {
    if (needle.isEmpty()) return null
    val start = indexOf(needle, ignoreCase = true)
    return if (start < 0) null else start until start + needle.length
}
