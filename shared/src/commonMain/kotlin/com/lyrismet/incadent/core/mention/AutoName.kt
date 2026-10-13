package com.lyrismet.incadent.core.mention

// a name a note can use without "@" - folded once, longest first so a surname form beats the bare name
internal class AutoName(
    val folded: String,
    val entity: MentionEntity,
)

internal fun autoNamesOf(candidates: List<MentionCandidate>): List<AutoName> =
    candidates
        .map { it.entity }
        .filter { it !is MentionEntity.DeletedMention }
        .distinctBy { it.dedupeKey() }
        .filter { it.name.length >= AUTO_MATCH_MIN_LENGTH }
        .map { AutoName(it.name.foldPerChar(), it) }
        .sortedByDescending { it.folded.length }

/** names shorter than this are too likely to be an ordinary word to be suggested as a mention */
const val AUTO_MATCH_MIN_LENGTH = 3

// lowercase and "ё" = "е" one char at a time, so every offset in the folded text is an offset in the original
internal fun String.foldPerChar(): String =
    buildString(length) {
        this@foldPerChar.forEach { char ->
            val lower = char.lowercaseChar()
            append(if (lower == 'ё') 'е' else lower)
        }
    }

// a whole word - not in the middle of a longer one and not the tail of an "@" token
internal fun List<AutoName>.matchAt(
    text: String,
    folded: String,
    index: Int,
): AutoName? {
    val startsWord = index == 0 || (!text[index - 1].isLetter() && text[index - 1] != '@')
    if (!startsWord) return null
    return firstOrNull { name ->
        val end = index + name.folded.length
        end <= folded.length &&
            folded.startsWith(name.folded, index) &&
            (end == text.length || !text[end].isLetter())
    }
}

/** the entities [segments] hold as plain words, first seen first - what "N matches with the codex" counts */
fun autoMatchedEntities(segments: List<MentionSegment>): List<MentionEntity> =
    segments
        .filterIsInstance<MentionSegment.AutoMatch>()
        .map { it.entity }
        .distinctBy { it.dedupeKey() }

/**
 * puts "@" in front of the auto matched words of [text] - only those of [only] when it is given. The word is
 * rewritten to the record's own spelling, so a case or "ё" difference is fixed on the way.
 */
fun linkAutoMentions(
    text: String,
    candidates: List<MentionCandidate>,
    questPrefix: String,
    only: MentionEntity? = null,
): String =
    parseMentions(text, candidates, autoMatch = true)
        .joinToString("") { segment ->
            val isWanted =
                only == null || only.dedupeKey() == (segment as? MentionSegment.AutoMatch)?.entity?.dedupeKey()
            if (segment is MentionSegment.AutoMatch && isWanted) {
                "@" + mentionKey(segment.entity, questPrefix)
            } else {
                segment.sourceText(questPrefix)
            }
        }

// the text a segment stood for - a mention is rebuilt from its own key, the key it was parsed from
private fun MentionSegment.sourceText(questPrefix: String): String =
    when (this) {
        is MentionSegment.Text -> text
        is MentionSegment.AutoMatch -> text
        is MentionSegment.Mention -> "@" + mentionKey(entity, questPrefix)
    }
