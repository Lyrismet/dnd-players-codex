package com.lyrismet.incadent.feature.mention.domain

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

/** in how many different sessions each record is mentioned, a record no note mentions is missing from the map */
fun mentionSessionCounts(
    bodies: List<MentionBody>,
    index: MentionIndex,
): Map<MentionRef, Int> =
    bodies
        .flatMap { body ->
            parseMentions(body.text, index)
                .filterIsInstance<MentionSegment.Mention>()
                .map { it.entity.ref to body.sessionId }
        }.distinct()
        .groupingBy { it.first }
        .eachCount()

/** turns the tokens of [from] into the token of the record [toRef] named [toName] */
fun relinkMentionIn(
    body: String,
    from: MentionRef,
    toRef: MentionRef,
    toName: String,
): String =
    TOKEN_REGEX.replace(body) {
        if (it.toRef() ==
            from
        ) {
            mentionToken(toRef, toName, it.quantity())
        } else {
            it.value
        }
    }
