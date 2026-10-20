package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.domain.model.EntityKind

// ids this high never belong to a stored record, so a token carrying one is a name nobody has made a record for yet
private const val UNLINKED_ID_BASE = 1_000_000_000_000L
private const val UNLINKED_ID_SPAN = 0x3FFFFFFFFFFFFFFFL
private const val FNV_OFFSET = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L

// a 64 bit hash, so two different names meeting in one ref is out of the question for a campaign's worth of names
private fun String.stableHash(): Long {
    var hash = FNV_OFFSET
    for (char in this) {
        hash = (hash xor char.code.toLong()) * FNV_PRIME
    }
    return hash
}

/** the stand-in ref of an "@Name" typed without choosing a record - the same name always gets the same ref */
fun unlinkedMentionRef(name: String) =
    MentionRef(EntityKind.NPC, UNLINKED_ID_BASE + (name.trim().foldForSearch().stableHash() and UNLINKED_ID_SPAN))

fun MentionRef.isUnlinked(): Boolean = id >= UNLINKED_ID_BASE

// an "@" that starts a word and its name up to a space
private val BARE_MENTION_REGEX = Regex("(^|[\\s(\"«])@([^@\\s\\[\\]|]+)")
private const val BARE_MENTION_TRAILING = ".,!?;:)\"»"

/**
 * turns each "@Name" still left in [text] after known names were linked into a token no record answers to,
 * a name that [index] holds more than one record of stays plain text
 */
fun markUnlinkedMentions(
    text: String,
    index: MentionIndex = MentionIndex.EMPTY,
): String {
    if ('@' !in text) return text
    val out = StringBuilder()
    var from = 0
    // the text inside an existing token is never touched
    for (token in TOKEN_REGEX.findAll(text)) {
        out.append(markBareMentions(text.substring(from, token.range.first), index))
        out.append(token.value)
        from = token.range.last + 1
    }
    out.append(markBareMentions(text.substring(from), index))
    return out.toString()
}

private fun markBareMentions(
    text: String,
    index: MentionIndex,
): String =
    BARE_MENTION_REGEX.replace(text) { match ->
        val raw = match.groupValues[2]
        val name = raw.trimEnd { it in BARE_MENTION_TRAILING }
        if (name.isEmpty() || index.hasName(name)) {
            match.value
        } else {
            match.groupValues[1] + mentionToken(unlinkedMentionRef(name), name) + raw.substring(name.length)
        }
    }

/** the reverse of [markUnlinkedMentions], "?" tags back to typed "@Name" so an edit can reach inside them */
fun unmarkUnlinkedMentions(text: String): String =
    if ("@[" !in text) {
        text
    } else {
        TOKEN_REGEX.replace(text) { if (it.toRef().isUnlinked()) "@" + it.tokenName() else it.value }
    }
