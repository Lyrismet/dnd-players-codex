package com.lyrismet.incadent.core.tags

/** one tag the catalog would suggest, with the reason shown under it (Players Codex v6.dc.html `tagSuggest`) */
data class TagSuggestion(
    val tag: String,
    val reason: String,
)

private sealed interface TagSignal {
    /** matched as the start of a word - "бой" also matches "боя", "бойня" */
    data class Prefix(
        val stem: String,
    ) : TagSignal

    /** matched only as a whole word - "зм" never matches inside "призма" */
    data class Exact(
        val value: String,
    ) : TagSignal

    /** the session mentions at least one quest (via @) - the only signal for "Сюжет" */
    data object QuestMentioned : TagSignal
}

private data class TagRule(
    val tag: String,
    val signals: List<TagSignal>,
)

// "Бой" also has a combat-entry signal in the mockup (entry.kind === 'combat'), skipped here - this codebase
// has no "бой" session-entry kind yet, that's part of the still-unstarted P3 combat feature
private val TAG_RULES =
    listOf(
        TagRule("Бой", listOf("бой", "боя", "атак", "засад", "сраж", "напал").map(TagSignal::Prefix)),
        TagRule("Сюжет", listOf(TagSignal.QuestMentioned)),
        TagRule(
            "Расследование",
            listOf("улик", "мёртв", "расслед", "допрос", "выясн", "загадк", "следы").map(TagSignal::Prefix),
        ),
        TagRule(
            "Интрига",
            listOf("сделк", "предал", "шпион", "синдикат", "заговор", "печать", "вынюхив").map(TagSignal::Prefix),
        ),
        TagRule(
            "Торговля",
            listOf("торг", "купил", "продал", "скидк").map(TagSignal::Prefix) + TagSignal.Exact("зм"),
        ),
        TagRule("Путешествие", listOf("добрал", "дорог", "лагер", "троп", "переход").map(TagSignal::Prefix)),
        TagRule("Отдых", listOf("отдых", "ночлег", "привал").map(TagSignal::Prefix)),
    )

private const val MAX_REASON_WORDS = 2

/**
 * on-device tag suggestions for a session, from its entry texts and its mentioned quests - never from an AI or
 * the network. A tag already in [assignedTags] is never suggested again. Pure and Compose-free, so a presenter
 * can call it directly from plain lists.
 */
fun suggestSessionTags(
    entryTexts: List<String>,
    mentionedQuestNames: List<String>,
    assignedTags: List<String>,
): List<TagSuggestion> {
    val combinedText = entryTexts.joinToString("\n")
    return TAG_RULES.mapNotNull { rule ->
        if (assignedTags.any { it.equals(rule.tag, ignoreCase = true) }) return@mapNotNull null
        reasonFor(rule, combinedText, mentionedQuestNames)?.let { TagSuggestion(rule.tag, it) }
    }
}

private fun reasonFor(
    rule: TagRule,
    combinedText: String,
    mentionedQuestNames: List<String>,
): String? =
    if (rule.signals.contains(TagSignal.QuestMentioned)) {
        questReason(mentionedQuestNames)
    } else {
        stemReason(combinedText, rule.signals)
    }

private fun questReason(mentionedQuestNames: List<String>): String? {
    if (mentionedQuestNames.isEmpty()) return null
    val prefix = if (mentionedQuestNames.size > 1) "Квесты: " else "Квест: "
    return prefix + mentionedQuestNames.take(MAX_REASON_WORDS).joinToString(", ")
}

private fun stemReason(
    text: String,
    signals: List<TagSignal>,
): String? {
    if (text.isBlank()) return null
    val words = signals.mapNotNull { firstMatch(text, it)?.lowercase() }.distinct().take(MAX_REASON_WORDS)
    return words.takeIf { it.isNotEmpty() }?.let { matched ->
        "Слова в записях: " + matched.joinToString(", ") { "«$it»" }
    }
}

private fun firstMatch(
    text: String,
    signal: TagSignal,
): String? =
    when (signal) {
        is TagSignal.Prefix -> signalRegex(signal.stem, exact = false).find(text)?.groupValues?.get(1)
        is TagSignal.Exact -> signalRegex(signal.value, exact = true).find(text)?.groupValues?.get(1)
        TagSignal.QuestMentioned -> null
    }

// mirrors the mockup's stemRe: a non-letter (or the string start) before, then either the whole word (exact)
// or the stem plus any following letters (prefix) - unicode-aware so cyrillic word boundaries work too
private fun signalRegex(
    value: String,
    exact: Boolean,
): Regex {
    val body = if (exact) "($value)(?![\\p{L}])" else "($value\\p{L}*)"
    return Regex("(?:^|[^\\p{L}])$body", RegexOption.IGNORE_CASE)
}
