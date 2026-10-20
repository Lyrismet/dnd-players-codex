package com.lyrismet.incadent.feature.codex.domain.model

private const val SEPARATOR = ","

/** splits what the player typed or the database holds into trimmed nicknames, blanks and repeats dropped */
fun parseNicknames(text: String): List<String> =
    text
        .split(SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

/** the comma separated text a nickname list is typed and stored as */
fun List<String>.joinNicknames(): String = joinToString("$SEPARATOR ")
