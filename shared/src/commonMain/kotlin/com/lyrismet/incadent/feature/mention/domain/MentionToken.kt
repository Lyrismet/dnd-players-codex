package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind

// the name shows once its record is gone, the kind letter and id decide who it is, the optional last number is how many
internal val TOKEN_REGEX = Regex("@\\[([^\\[\\]|]*)\\|([npqls]):(\\d+)(?:\\|(\\d+))?]")

internal fun EntityKind.code(): Char =
    when (this) {
        EntityKind.NPC -> 'n'
        EntityKind.PARTY -> 'p'
        EntityKind.LOCATION -> 'l'
        EntityKind.QUEST -> 'q'
        EntityKind.SPECIES -> 's'
    }

private fun Char.toMentionType(): EntityKind =
    when (this) {
        'n' -> EntityKind.NPC
        'p' -> EntityKind.PARTY
        'l' -> EntityKind.LOCATION
        's' -> EntityKind.SPECIES
        else -> EntityKind.QUEST
    }

internal const val NAME_GROUP = 1
private const val KIND_GROUP = 2
private const val ID_GROUP = 3
internal const val QUANTITY_GROUP = 4

internal fun MatchResult.toRef() =
    MentionRef(groupValues[KIND_GROUP].single().toMentionType(), groupValues[ID_GROUP].toLong())

/** how many a species token stands for, 1 for any other kind or when the token carries no number */
internal fun MatchResult.quantity(): Int =
    if (toRef().type != EntityKind.SPECIES) {
        1
    } else {
        groupValues[QUANTITY_GROUP].toIntOrNull()?.coerceIn(1, MAX_MENTION_QUANTITY) ?: 1
    }

// no spaces inside a token, so a note's words still split cleanly when phrases are cut out of it
private val TOKEN_NAME_BREAKS = Regex("[\\[\\]|\\s]+")

internal fun MatchResult.tokenName(): String = groupValues[NAME_GROUP].replace('_', ' ')

/** the text stored in a note for a mention of [ref], the name written without spaces, brackets and bars */
fun mentionToken(
    ref: MentionRef,
    name: String,
    quantity: Int = 1,
): String {
    val count =
        if (ref.type == EntityKind.SPECIES &&
            quantity > 1
        ) {
            "|${quantity.coerceAtMost(MAX_MENTION_QUANTITY)}"
        } else {
            ""
        }
    return "@[${name.replace(TOKEN_NAME_BREAKS, "_")}|${ref.type.code()}:${ref.id}$count]"
}

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
                index.uniqueByName.firstOrNull { text.regionMatchesFolded(i + 1, it.name) }
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
): String = relinkMentionIn(body, from, to.ref, to.name)

/** turns the tokens of [from] into the plain name they carry - a "?" tag becoming words */
fun unlinkMentionIn(
    body: String,
    from: MentionRef,
): String = TOKEN_REGEX.replace(body) { if (it.toRef() == from) it.tokenName() + it.countSuffix() else it.value }
