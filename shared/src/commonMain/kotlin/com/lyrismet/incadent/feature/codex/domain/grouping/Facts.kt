package com.lyrismet.incadent.feature.codex.domain.grouping

private const val FACT_SEPARATOR = '\n'

/** the facts as the text a column stores, one per line */
fun List<String>.encodeFacts(): String = joinToString(FACT_SEPARATOR.toString())

/** reads what [encodeFacts] wrote, a blank line is no fact */
fun decodeFacts(raw: String): List<String> =
    if (raw.isEmpty()) emptyList() else raw.split(FACT_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

/** the list with [fact] added at the end, a fact the list already has (any letter case) is not added again */
fun List<String>.withFact(fact: String): List<String> {
    val clean = fact.trim().replace(FACT_SEPARATOR, ' ')
    return if (clean.isEmpty() || any { it.equals(clean, ignoreCase = true) }) this else this + clean
}
