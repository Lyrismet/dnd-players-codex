package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.EntityKind

// "@[Name|n:12]" - the name is only what a tag shows once its record is gone, the kind letter and id decide who it is
internal val TOKEN_REGEX = Regex("@\\[([^\\[\\]|]*)\\|([npql]):(\\d+)]")

private fun EntityKind.code(): Char =
    when (this) {
        EntityKind.NPC -> 'n'
        EntityKind.PARTY -> 'p'
        EntityKind.LOCATION -> 'l'
        EntityKind.QUEST -> 'q'
    }

private fun Char.toMentionType(): EntityKind =
    when (this) {
        'n' -> EntityKind.NPC
        'p' -> EntityKind.PARTY
        'l' -> EntityKind.LOCATION
        else -> EntityKind.QUEST
    }

internal const val NAME_GROUP = 1
private const val KIND_GROUP = 2
private const val ID_GROUP = 3

internal fun MatchResult.toRef() =
    MentionRef(groupValues[KIND_GROUP].single().toMentionType(), groupValues[ID_GROUP].toLong())

// no spaces inside a token, so a note's words still split cleanly when phrases are cut out of it
private val TOKEN_NAME_BREAKS = Regex("[\\[\\]|\\s]+")

internal fun MatchResult.tokenName(): String = groupValues[NAME_GROUP].replace('_', ' ')

/** the text stored in a note for a mention of [ref], the name written without spaces, brackets and bars */
fun mentionToken(
    ref: MentionRef,
    name: String,
): String = "@[${name.replace(TOKEN_NAME_BREAKS, "_")}|${ref.type.code()}:${ref.id}]"

fun MentionEntity.token(): String = mentionToken(ref, name)

/** turns every "@Name" a note was typed with into the token of the record that name belongs to, longest name first */
fun linkTypedMentions(
    text: String,
    index: MentionIndex,
): String {
    if ('@' !in text || index.uniqueByName.isEmpty()) return text
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        val typed =
            if (text[i] == '@' && !text.startsWith("@[", i)) {
                index.uniqueByName.firstOrNull { text.startsWith(it.name, i + 1) }
            } else {
                null
            }
        if (typed != null) {
            out.append(typed.token())
            i += 1 + typed.name.length
        } else {
            out.append(text[i])
            i++
        }
    }
    return out.toString()
}

/** the note text with every token replaced by the name it carries - for labels, snippets and keyword rules */
fun plainMentionText(body: String): String = TOKEN_REGEX.replace(body) { it.tokenName() }

/** turns the tokens of [from] into [to]'s own token, the "?" tag of a deleted record becoming a link */
fun relinkMentionIn(
    body: String,
    from: MentionRef,
    to: MentionEntity,
): String = TOKEN_REGEX.replace(body) { if (it.toRef() == from) to.token() else it.value }

/** turns the tokens of [from] into the plain name they carry - a "?" tag becoming words */
fun unlinkMentionIn(
    body: String,
    from: MentionRef,
): String = TOKEN_REGEX.replace(body) { if (it.toRef() == from) it.tokenName() else it.value }

// what is left of a token after an edit cut into it
private val TOKEN_FRAGMENT_REGEX = Regex("@\\[[^\\[\\]@]*?\\|[npql]:\\d*]?")

/** the draft without any half deleted token, so an edit inside a tag removes the tag instead of corrupting it */
fun withoutBrokenTokens(text: String): String {
    if ("@[" !in text) return text
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (!text.startsWith("@[", i)) {
            out.append(text[i])
            i++
            continue
        }
        val whole = TOKEN_REGEX.matchAt(text, i)
        if (whole != null) {
            out.append(whole.value)
            i += whole.value.length
        } else {
            // a cut token goes whole, a stray "@[" alone loses just its two characters
            i += TOKEN_FRAGMENT_REGEX.matchAt(text, i)?.value?.length ?: 2
        }
    }
    return out.toString()
}
