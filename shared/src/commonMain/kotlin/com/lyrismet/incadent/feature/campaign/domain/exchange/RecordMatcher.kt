package com.lyrismet.incadent.feature.campaign.domain.exchange

private const val MIN_SIMILAR_CHARS = 3

/** a record of the open campaign a file's record may be the same as */
data class LocalRecord(
    val id: Long,
    val name: String,
    val originId: String,
)

/** what a record of a file turned out to be next to the ones already in the campaign */
sealed interface RecordMatch {
    /** the origin id (or an alias of it) is the campaign's own */
    data class ByOrigin(
        val id: Long,
    ) : RecordMatch

    /** the same name and kind, an exact twin made on its own */
    data class ByName(
        val id: Long,
    ) : RecordMatch

    /** one name holds the other ("Кахури" and "Деревня Кахури"), only a person can tell */
    data class Similar(
        val id: Long,
        val name: String,
    ) : RecordMatch

    data object None : RecordMatch
}

/**
 * finds the record of [locals] a file's record stands for: the origin id first ([resolveAlias] turns an id merged
 * away into the survivor's), then the exact name, then a name that contains the other. A pair in [apart]
 * (file origin id to local origin id) was told to be different and is never offered again.
 */
@Suppress("ReturnCount")
fun matchRecord(
    fileOriginId: String?,
    fileName: String,
    locals: List<LocalRecord>,
    apart: Set<Pair<String, String>> = emptySet(),
    resolveAlias: (String) -> String = { it },
): RecordMatch {
    val origin = fileOriginId?.takeIf { it.isNotBlank() }
    if (origin != null) {
        val wanted = resolveAlias(origin)
        locals.firstOrNull { it.originId == wanted }?.let { return RecordMatch.ByOrigin(it.id) }
    }
    val candidates = locals.filter { origin == null || (origin to it.originId) !in apart }
    val key = fileName.nameKey()
    candidates
        .filter { it.name.nameKey() == key }
        .minByOrNull { it.id }
        ?.let { return RecordMatch.ByName(it.id) }
    if (key.length < MIN_SIMILAR_CHARS) return RecordMatch.None
    return candidates
        .filter {
            val other = it.name.nameKey()
            other.length >= MIN_SIMILAR_CHARS && (other.contains(key) || key.contains(other))
        }.minByOrNull { it.id }
        ?.let { RecordMatch.Similar(it.id, it.name) } ?: RecordMatch.None
}
