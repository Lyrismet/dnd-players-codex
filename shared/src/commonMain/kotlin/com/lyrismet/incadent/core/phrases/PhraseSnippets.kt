package com.lyrismet.incadent.core.phrases

/** one phrase offered above the composer - a pinned snippet (★) or an automatically found frequent one (↻ ×N) */
data class PhraseOption(
    val text: String,
    val isPinned: Boolean,
    val entryCount: Int = 0,
)

/** what the player has typed since the last sentence end, and the phrases that continue it */
data class PhraseCompletions(
    val fragment: String,
    val options: List<PhraseOption>,
)

private const val MIN_FRAGMENT_LENGTH = 3
private const val MAX_COMPLETIONS = 4
private const val MAX_SNIPPET_LENGTH = 200
private val COMPLETION_BREAKS = charArrayOf('.', '!', '?', '\n')
private val WHITESPACE_RUN = Regex("\\s+")

/** pinned snippets first, then the frequent phrases that are not pinned yet */
fun phrasePool(
    pinned: List<String>,
    frequent: List<FrequentPhrase>,
): List<PhraseOption> =
    pinned.map { PhraseOption(it, isPinned = true) } +
        frequent
            .filter { phrase -> pinned.none { it == phrase.text } }
            .map { PhraseOption(it.text, isPinned = false, entryCount = it.entryCount) }

/** the part of [draft] typed since the last sentence end, or null while it is too short to continue */
fun phraseFragment(draft: String): String? {
    if (draft.isBlank()) return null
    val fragment = draft.substring(draft.lastIndexOfAny(COMPLETION_BREAKS) + 1).trimStart()
    return fragment.takeIf { it.length >= MIN_FRAGMENT_LENGTH }
}

/** pool phrases that start with the current sentence fragment of [draft] - null while nothing sensible is typed */
fun phraseCompletions(
    draft: String,
    pool: List<PhraseOption>,
): PhraseCompletions? {
    val fragment = phraseFragment(draft) ?: return null
    val typedLength = fragment.trim().length
    val options =
        pool
            .filter { it.text.startsWith(fragment, ignoreCase = true) && it.text.length > typedLength }
            .take(MAX_COMPLETIONS)
    return options.takeIf { it.isNotEmpty() }?.let { PhraseCompletions(fragment, it) }
}

/** appends [phrase] to [draft], or swaps the typed [replacing] fragment for it - always ends with a space */
fun insertPhrase(
    draft: String,
    phrase: String,
    replacing: String? = null,
): String {
    val base =
        when {
            replacing != null -> draft.dropLast(replacing.length)
            draft.isNotEmpty() && !draft.last().isWhitespace() -> "$draft "
            else -> draft
        }
    return "$base$phrase "
}

/** the phrase as plain text for a chip or row - mention markers dropped, cut to [maxLength] with an ellipsis */
fun phraseLabel(
    text: String,
    questPrefix: String,
    maxLength: Int? = null,
): String {
    val plain = text.replace("@$questPrefix", "").replace("@", "")
    return if (maxLength != null && plain.length > maxLength) plain.take(maxLength - 1) + "…" else plain
}

/** a note as a one-line snippet - whitespace collapsed, cut at a word boundary when longer than the limit */
fun snippetFromEntry(body: String): String {
    val flat = body.trim().replace(WHITESPACE_RUN, " ")
    if (flat.length <= MAX_SNIPPET_LENGTH) return flat
    val cut = flat.take(MAX_SNIPPET_LENGTH)
    return cut.substringBeforeLast(' ', missingDelimiterValue = cut)
}
