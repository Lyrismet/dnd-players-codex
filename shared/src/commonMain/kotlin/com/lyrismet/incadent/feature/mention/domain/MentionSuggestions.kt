package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.domain.model.EntityKind

private const val SEARCH_LIMIT = 5
private const val EMPTY_QUERY_LIMIT = 7

/** one row of the "@" dropdown - [nickname] is set when the query matched a nickname and not the name */
data class MentionCandidate(
    val entity: MentionEntity,
    val nickname: String? = null,
)

/** where a row of the empty "@" dropdown comes from */
enum class MentionSection { THIS_SESSION, PREVIOUS_SESSION, RECENT, CODEX }

data class MentionSectionRow(
    val section: MentionSection,
    val candidate: MentionCandidate,
)

/** what the dropdown shows for the text after "@" - the found records and whether to offer making a new one */
data class MentionSearch(
    val candidates: List<MentionCandidate>,
    val createName: String?,
)

/** records whose name or nickname starts with or contains [rawQuery], names before nicknames, [preferred] kind first */
fun searchMentions(
    entities: List<MentionEntity>,
    nicknames: Map<MentionRef, List<String>>,
    rawQuery: String,
    preferred: EntityKind? = null,
): MentionSearch {
    val query = rawQuery.trim().foldForSearch()
    val found =
        entities
            .mapNotNull { entity ->
                matchRank(entity, nicknames[entity.ref].orEmpty(), query)?.let { entity to it }
            }.sortedWith(
                compareBy<Pair<MentionEntity, MentionMatch>>(
                    { if (it.first.ref.type == preferred) 0 else 1 },
                    { it.second.rank },
                ),
            ).take(SEARCH_LIMIT)
            .map { (entity, match) -> MentionCandidate(entity, match.nickname) }
    val hasExactName = entities.any { it.name.foldForSearch() == query }
    val name = rawQuery.trim()
    return MentionSearch(found, name.takeIf { it.isNotEmpty() && !hasExactName })
}

private const val RANK_NAME_START = 0
private const val RANK_NICKNAME_START = 1
private const val RANK_NAME_INSIDE = 2
private const val RANK_NICKNAME_INSIDE = 3

private class MentionMatch(
    val rank: Int,
    val nickname: String?,
)

private fun matchRank(
    entity: MentionEntity,
    nicknames: List<String>,
    query: String,
): MentionMatch? {
    val name = entity.name.foldForSearch()
    return when {
        query.isEmpty() -> null
        name.startsWith(query) -> MentionMatch(RANK_NAME_START, null)
        query.length > 1 && name.contains(query) -> MentionMatch(RANK_NAME_INSIDE, null)
        else -> nicknameMatch(nicknames, query)
    }
}

private fun nicknameMatch(
    nicknames: List<String>,
    query: String,
): MentionMatch? {
    val starts = nicknames.firstOrNull { it.foldForSearch().startsWith(query) }
    val inside = nicknames.firstOrNull { query.length > 1 && it.foldForSearch().contains(query) }
    return when {
        starts != null -> MentionMatch(RANK_NICKNAME_START, starts)
        inside != null -> MentionMatch(RANK_NICKNAME_INSIDE, inside)
        else -> null
    }
}

/**
 * the rows of an empty "@": records of this session, then of the previous one, then the newest records;
 * the whole codex by name when none of those exist
 */
fun emptyMentionRows(
    thisSession: List<MentionEntity>,
    previousSession: List<MentionEntity>,
    recentlyCreated: List<MentionEntity>,
    all: List<MentionEntity>,
    preferred: EntityKind? = null,
): List<MentionSectionRow> {
    val seen = mutableSetOf<MentionRef>()
    val rows = mutableListOf<MentionSectionRow>()

    fun add(
        section: MentionSection,
        list: List<MentionEntity>,
    ) {
        val ordered = list.filter { it.ref.type == preferred } + list.filter { it.ref.type != preferred }
        for (entity in ordered) {
            if (rows.size >= EMPTY_QUERY_LIMIT) return
            if (seen.add(entity.ref)) rows += MentionSectionRow(section, MentionCandidate(entity))
        }
    }
    add(MentionSection.THIS_SESSION, thisSession)
    add(MentionSection.PREVIOUS_SESSION, previousSession)
    add(MentionSection.RECENT, recentlyCreated)
    if (rows.isEmpty()) add(MentionSection.CODEX, all.sortedBy { it.name.foldForSearch() })
    return rows
}
