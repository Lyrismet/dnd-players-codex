package com.lyrismet.incadent.core.phrases

import com.lyrismet.incadent.domain.model.SessionEntry

/** a word chain the player keeps writing - found in [entryCount] different notes across [sessionCount] sessions */
data class FrequentPhrase(
    val text: String,
    val entryCount: Int,
    val sessionCount: Int,
)

private const val MIN_PHRASE_WORDS = 4
private const val MAX_PHRASE_WORDS = 12
private const val MIN_ENTRY_COUNT = 2
const val FREQUENT_PHRASE_LIMIT = 6

private val WHITESPACE = Regex("\\s+")
private val SENTENCE_ENDS = setOf('.', '!', '?')
private val KEY_TRAILING_PUNCTUATION = setOf('.', ',', ':', ';', '!', '?', '«', '»', '(', ')')
private val TEXT_TRAILING_PUNCTUATION = setOf('.', ',', ':', ';', '!', '?')

private class PhraseStat(
    val key: String,
    val text: String,
    val wordCount: Int,
) {
    val entryIds = mutableSetOf<Long>()
    val sessionIds = mutableSetOf<Long>()
}

private class Sentence(
    val entry: SessionEntry,
    val words: List<String>,
) {
    val normalized: List<String> = words.map { it.lowercase().trimEnd { char -> char in KEY_TRAILING_PUNCTUATION } }

    fun key(
        start: Int,
        size: Int,
    ): String = normalized.subList(start, start + size).joinToString(" ")
}

/**
 * on-device search for repeating chains of 4+ words (Players Codex v6.dc.html `minePhrases`) - notes are cut into
 * sentences, a chain counts once per note, and a chain wholly inside a longer found one is dropped unless it is
 * found in more notes than that one. No AI, no network.
 */
fun findFrequentPhrases(
    entries: List<SessionEntry>,
    limit: Int = FREQUENT_PHRASE_LIMIT,
): List<FrequentPhrase> {
    val sentences = entries.flatMap { entry -> sentencesOf(entry.body).map { Sentence(entry, it) } }
    val repeatedStarts = repeatedChainStarts(sentences)
    val stats = mutableMapOf<String, PhraseStat>()
    sentences.forEach { collectChains(it, repeatedStarts, stats) }

    val longestFirst =
        stats.values
            .filter { it.entryIds.size >= MIN_ENTRY_COUNT }
            .sortedWith(compareByDescending<PhraseStat> { it.wordCount }.thenByDescending { it.entryIds.size })
    val kept = mutableListOf<PhraseStat>()
    longestFirst.forEach { stat -> if (kept.none { it.swallows(stat) }) kept += stat }

    return kept
        .sortedWith(compareByDescending<PhraseStat> { it.entryIds.size }.thenByDescending { it.wordCount })
        .take(limit)
        .map { FrequentPhrase(it.text, it.entryIds.size, it.sessionIds.size) }
}

/** a longer chain hides a shorter one made of whole words of it, unless the shorter is found in more notes */
private fun PhraseStat.swallows(other: PhraseStat): Boolean =
    " $key ".contains(" ${other.key} ") && entryIds.size >= other.entryIds.size

/** a repeating chain starts with a repeating minimum-length chain, so only those starts are worth expanding */
private fun repeatedChainStarts(sentences: List<Sentence>): Set<String> {
    val entryIdsByKey = mutableMapOf<String, MutableSet<Long>>()
    sentences.forEach { sentence ->
        for (start in 0..sentence.words.size - MIN_PHRASE_WORDS) {
            entryIdsByKey.getOrPut(sentence.key(start, MIN_PHRASE_WORDS)) { mutableSetOf() } += sentence.entry.id
        }
    }
    return entryIdsByKey.filterValues { it.size >= MIN_ENTRY_COUNT }.keys
}

private fun collectChains(
    sentence: Sentence,
    repeatedStarts: Set<String>,
    stats: MutableMap<String, PhraseStat>,
) {
    for (start in 0..sentence.words.size - MIN_PHRASE_WORDS) {
        if (sentence.key(start, MIN_PHRASE_WORDS) !in repeatedStarts) continue
        for (size in MIN_PHRASE_WORDS..minOf(MAX_PHRASE_WORDS, sentence.words.size - start)) {
            val stat =
                stats.getOrPut(sentence.key(start, size)) {
                    val text = sentence.words.subList(start, start + size).joinToString(" ")
                    PhraseStat(sentence.key(start, size), text.trimEnd { it in TEXT_TRAILING_PUNCTUATION }, size)
                }
            stat.entryIds += sentence.entry.id
            stat.sessionIds += sentence.entry.sessionNoteId
        }
    }
}

/** a sentence ends at a word closed with . ! or ? and at every line break */
private fun sentencesOf(text: String): List<List<String>> =
    text.lines().flatMap { line ->
        buildList {
            val current = mutableListOf<String>()
            line.split(WHITESPACE).filter { it.isNotEmpty() }.forEach { word ->
                current += word
                if (word.last() in SENTENCE_ENDS) {
                    add(current.toList())
                    current.clear()
                }
            }
            if (current.isNotEmpty()) add(current.toList())
        }
    }
