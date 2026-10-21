package com.lyrismet.incadent.core.selectlist

import com.lyrismet.incadent.core.format.collationKey

/** one entry of a select list - [searchText] is what a query is matched against, usually the title plus the subtitle */
data class SelectOption(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val searchText: String = title,
    /** why the option can't be picked - it is listed greyed at the end with this note, null for a pickable one */
    val unavailableNote: String? = null,
    /** options that carry this one along when checked - it then can't be checked on its own */
    val coveredBy: List<SelectCover> = emptyList(),
    /** the subheading the option is listed under, groups keep the order they first appear in [selectRows]'s options */
    val group: String? = null,
)

/** an option that, while checked, carries the options naming it in [SelectOption.coveredBy] */
data class SelectCover(
    val id: Long,
    val name: String,
)

/** a shown option, [match] is the part of the title the query hit - null when the hit is only in the subtitle */
data class SelectRow(
    val option: SelectOption,
    val selected: Boolean,
    val match: IntRange? = null,
    /** the name of the checked option that carries this one along, null when it stands alone */
    val carriedBy: String? = null,
) {
    val isInactive: Boolean get() = option.unavailableNote != null || carriedBy != null
}

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
    val groupRank =
        options
            .mapNotNull { it.group }
            .distinct()
            .withIndex()
            .associate { (index, name) -> name to index }
    return options
        .asSequence()
        .filter { it.id !in excludedIds }
        .filter { needle.isEmpty() || it.searchText.contains(needle, ignoreCase = true) }
        .sortedWith(
            compareBy<SelectOption>(
                { groupRank[it.group] ?: -1 },
                { if (it.unavailableNote != null) 1 else 0 },
                { if (floatSelected && needle.isEmpty() && it.id in selectedIds) 0 else 1 },
                { recentRank[it.id] ?: Int.MAX_VALUE },
                { it.title.collationKey() },
                { it.id },
            ),
        ).map { option ->
            val carrier = option.coveredBy.firstOrNull { it.id in selectedIds }
            SelectRow(
                option,
                option.id in selectedIds && carrier == null,
                option.title.matchRange(needle),
                carrier?.name,
            )
        }.toList()
}

/** the ids that count as picked - a checked option carried along by another checked one rides with it */
fun pickedIds(
    options: List<SelectOption>,
    checked: Set<Long>,
): Set<Long> {
    val carried = options.filter { option -> option.id in checked && option.coveredBy.any { it.id in checked } }
    return checked - carried.map { it.id }.toSet()
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
