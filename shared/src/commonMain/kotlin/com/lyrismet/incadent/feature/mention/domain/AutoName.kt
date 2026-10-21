package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind

// a name a note can use without "@" - folded once, longest first so a surname form beats the bare name
internal class AutoName(
    val folded: String,
    val entity: MentionEntity,
)

internal fun autoNamesOf(index: MentionIndex): List<AutoName> =
    index.uniqueByName
        .filter { it.name.length >= AUTO_MATCH_MIN_LENGTH && it.ref.type != EntityKind.SPECIES }
        .map { AutoName(it.name.foldPerChar(), it) }
        .sortedByDescending { it.folded.length }

/** names shorter than this are too likely to be an ordinary word to be suggested as a mention */
const val AUTO_MATCH_MIN_LENGTH = 3

// folds case and the yo letter one char at a time so folded offsets match the original
internal fun String.foldPerChar(): String =
    buildString(length) {
        this@foldPerChar.forEach { char ->
            val lower = char.lowercaseChar()
            append(if (lower == 'ё') 'е' else lower)
        }
    }

// the same letters whatever their case, the yo letter counting as ye
internal fun String.regionMatchesFolded(
    offset: Int,
    name: String,
): Boolean =
    offset + name.length <= length && substring(offset, offset + name.length).foldPerChar() == name.foldPerChar()

// a whole word - not in the middle of a longer one or of a number and not the tail of an "@" token
internal fun List<AutoName>.matchAt(
    text: String,
    folded: String,
    index: Int,
): AutoName? {
    val startsWord = index == 0 || (!text[index - 1].isLetterOrDigit() && text[index - 1] != '@')
    if (!startsWord) return null
    return firstOrNull { name ->
        val end = index + name.folded.length
        end <= folded.length &&
            folded.startsWith(name.folded, index) &&
            (end == text.length || !text[end].isLetterOrDigit())
    }
}

/** the entities [segments] hold as plain words, first seen first - what "N matches with the codex" counts */
fun autoMatchedEntities(segments: List<MentionSegment>): List<MentionEntity> =
    segments
        .filterIsInstance<MentionSegment.AutoMatch>()
        .map { it.entity }
        .distinctBy { it.dedupeKey() }

/** turns the auto matched words of [text] into mention tokens, only those of [only] when given */
fun linkAutoMentions(
    text: String,
    index: MentionIndex,
    only: MentionEntity? = null,
): String =
    parseMentions(text, index, autoMatch = true)
        .joinToString("") { segment ->
            when (segment) {
                is MentionSegment.Text -> segment.text
                is MentionSegment.Mention -> mentionToken(segment.entity.ref, segment.entity.name, segment.quantity)
                is MentionSegment.AutoMatch ->
                    if (only == null || only.dedupeKey() == segment.entity.dedupeKey()) {
                        segment.entity.token()
                    } else {
                        segment.text
                    }
            }
        }
