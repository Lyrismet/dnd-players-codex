package com.lyrismet.incadent.core.mention

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
    index: MentionIndex,
): MentionHits {
    val target = entity.dedupeKey()
    val hit =
        bodies.filter { body ->
            parseMentions(body.text, index).any {
                it is MentionSegment.Mention && it.entity.dedupeKey() == target
            }
        }
    return MentionHits(notes = hit.size, sessions = hit.map { it.sessionId }.distinct().size)
}
