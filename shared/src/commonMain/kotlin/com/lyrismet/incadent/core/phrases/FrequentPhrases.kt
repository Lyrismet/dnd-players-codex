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
private val SENTENCE_ENDS = charArrayOf('.', '!', '?')
private val KEY_TRAILING_PUNCTUATION = charArrayOf('.', ',', ':', ';', '!', '?', '«', '»', '(', ')')
private val TEXT_TRAILING_PUNCTUATION = charArrayOf('.', ',', ':', ';', '!', '?')

private class PhraseStat(
    val key: String,
    val text: String,
    val wordCount: Int,
) {
    val entryIds = mutableSetOf<Long>()
    val sessionIds = mutableSetOf<Long>()
}

/**
 * on-device search for repeating chains of 4+ words (Players Codex v6.dc.html `minePhrases`) - notes are cut into
 * sentences, a chain counts once per note, and a chain wholly inside a longer found one is dropped. No AI, no network.
 */
fun findFrequentPhrases(
    entries: List<SessionEntry>,
    limit: Int = FREQUENT_PHRASE_LIMIT,
): List<FrequentPhrase> {
    val stats = mutableMapOf<String, PhraseStat>()
    entries.forEach { entry -> collectChains(entry, stats) }

    val longestFirst =
        stats.values
            .filter { it.entryIds.size >= MIN_ENTRY_COUNT }
            .sortedWith(compareByDescending<PhraseStat> { it.wordCount }.thenByDescending { it.entryIds.size })
    val kept = mutableListOf<PhraseStat>()
    longestFirst.forEach { stat -> if (kept.none { it.key.contains(stat.key) }) kept += stat }

    return kept
        .sortedWith(compareByDescending<PhraseStat> { it.entryIds.size }.thenByDescending { it.wordCount })
        .take(limit)
        .map { FrequentPhrase(it.text, it.entryIds.size, it.sessionIds.size) }
}

private fun collectChains(
    entry: SessionEntry,
    stats: MutableMap<String, PhraseStat>,
) {
    sentencesOf(entry.body).forEach { words ->
        val normalized = words.map { it.lowercase().trimEnd(*KEY_TRAILING_PUNCTUATION) }
        for (size in minOf(MAX_PHRASE_WORDS, words.size) downTo MIN_PHRASE_WORDS) {
            for (start in 0..words.size - size) {
                val key = normalized.subList(start, start + size).joinToString(" ")
                val stat =
                    stats.getOrPut(key) {
                        val text = words.subList(start, start + size).joinToString(" ")
                        PhraseStat(key, text.trimEnd(*TEXT_TRAILING_PUNCTUATION), size)
                    }
                stat.entryIds += entry.id
                stat.sessionIds += entry.sessionNoteId
            }
        }
    }
}

/** a sentence ends at a word closed with . ! or ? and at every line break */
private fun sentencesOf(text: String): List<List<String>> =
    text.lines().flatMap { line ->
        val sentences = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        line.split(WHITESPACE).filter { it.isNotEmpty() }.forEach { word ->
            current += word
            if (word.last() in SENTENCE_ENDS) {
                sentences += current
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) sentences += current
        sentences
    }
