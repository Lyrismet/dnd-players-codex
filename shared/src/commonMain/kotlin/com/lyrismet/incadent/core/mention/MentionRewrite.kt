package com.lyrismet.incadent.core.mention

/** rewrites "@oldKey" to "@newKey" in [body], skipping mentions of a longer key in [otherKeys] that starts with it */
fun renameMentionIn(
    body: String,
    oldKey: String,
    newKey: String,
    otherKeys: List<String>,
): String = replaceMentionIn(body, oldKey, "@$newKey", otherKeys)

/** turns every "@key" into [plainText] with the longer-key rule of [renameMentionIn] - a "?" tag becoming words */
fun unlinkMentionIn(
    body: String,
    key: String,
    plainText: String,
    otherKeys: List<String>,
): String = replaceMentionIn(body, key, plainText, otherKeys)

private fun replaceMentionIn(
    body: String,
    oldKey: String,
    replacement: String,
    otherKeys: List<String>,
): String {
    if (oldKey.isEmpty() || '@' !in body) return body
    val longestOther = otherKeys.filter { it.length > oldKey.length }
    val out = StringBuilder()
    var i = 0
    while (i < body.length) {
        val isOld = body[i] == '@' && body.startsWith(oldKey, i + 1) && longestOther.none { body.startsWith(it, i + 1) }
        if (isOld) {
            out.append(replacement)
            i += 1 + oldKey.length
        } else {
            out.append(body[i])
            i++
        }
    }
    return out.toString()
}

/** how many notes and how many different sessions mention one record - the numbers the delete warning quotes */
data class MentionHits(
    val notes: Int,
    val sessions: Int,
)

/** one note's text and the session it sits in */
data class MentionBody(
    val sessionId: Long,
    val text: String,
)

fun countMentionHits(
    entity: MentionEntity,
    bodies: List<MentionBody>,
    candidates: List<MentionCandidate>,
): MentionHits {
    val target = entity.dedupeKey()
    val hit =
        bodies.filter { body ->
            parseMentions(body.text, candidates).any {
                it is MentionSegment.Mention && it.entity.dedupeKey() == target
            }
        }
    return MentionHits(notes = hit.size, sessions = hit.map { it.sessionId }.distinct().size)
}
