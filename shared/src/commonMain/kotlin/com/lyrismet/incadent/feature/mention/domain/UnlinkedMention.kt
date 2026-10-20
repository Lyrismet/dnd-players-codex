package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.domain.model.EntityKind

// ids this high never belong to a stored record, so a token carrying one is a name nobody has made a record for yet
private const val UNLINKED_ID_BASE = 1_000_000_000_000L
private const val UNLINKED_ID_SPAN = 0x7FFFFFFFL

/** the stand-in ref of an "@Name" typed without choosing a record - the same name always gets the same ref */
fun unlinkedMentionRef(name: String) =
    MentionRef(
        EntityKind.NPC,
        UNLINKED_ID_BASE + (
            name
                .trim()
                .foldForSearch()
                .hashCode()
                .toLong() and UNLINKED_ID_SPAN
        ),
    )

fun MentionRef.isUnlinked(): Boolean = id >= UNLINKED_ID_BASE

// an "@" that starts a word, its name up to a space, and not already the "@[" of a token
private val BARE_MENTION_REGEX = Regex("(^|[\\s(\"«])@([^@\\s\\[\\]|]{1,24})")
private const val BARE_MENTION_TRAILING = ".,!?;:)\"»"

/** turns each "@Name" still left in [text] after known names were linked into a token no record answers to */
fun markUnlinkedMentions(text: String): String {
    if ('@' !in text) return text
    return BARE_MENTION_REGEX.replace(text) { match ->
        val raw = match.groupValues[2]
        val name = raw.trimEnd { it in BARE_MENTION_TRAILING }
        if (name.isEmpty()) {
            match.value
        } else {
            match.groupValues[1] + mentionToken(unlinkedMentionRef(name), name) + raw.substring(name.length)
        }
    }
}

/** the reverse of [markUnlinkedMentions], "?" tags back to typed "@Name" so an edit can reach inside them */
fun unmarkUnlinkedMentions(text: String): String =
    if ("@[" !in text) {
        text
    } else {
        TOKEN_REGEX.replace(text) { if (it.toRef().isUnlinked()) "@" + it.tokenName() else it.value }
    }
