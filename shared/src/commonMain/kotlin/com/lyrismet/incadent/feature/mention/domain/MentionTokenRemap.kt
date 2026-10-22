package com.lyrismet.incadent.feature.mention.domain

// no row ever gets id 0, so a token pointing there reads as the "?" tag of a record that is gone
const val UNKNOWN_RECORD_ID = 0L

/** moves every token to the id [newId] gives it, a record it knows nothing about becomes a "?" tag, the count stays */
fun remapMentionTokens(
    body: String,
    newId: (MentionRef) -> Long?,
): String =
    TOKEN_REGEX.replace(body) { match ->
        val ref = match.toRef()
        val count = match.groupValues[QUANTITY_GROUP].let { if (it.isEmpty()) "" else "|$it" }
        "@[${match.groupValues[NAME_GROUP]}|${ref.type.code()}:${newId(ref) ?: UNKNOWN_RECORD_ID}$count]"
    }

/** every record a note body points at, in order and with repeats */
fun mentionRefsIn(body: String): List<MentionRef> = TOKEN_REGEX.findAll(body).map { it.toRef() }.toList()
