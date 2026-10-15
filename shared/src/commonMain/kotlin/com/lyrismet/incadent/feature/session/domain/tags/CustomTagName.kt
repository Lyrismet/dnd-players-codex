package com.lyrismet.incadent.feature.session.domain.tags

/** the "СВОЙ ТЕГ" field's save rule - trimmed, first letter capitalized, or null when blank (never saved) */
fun normalizeCustomTagName(draft: String): String? {
    val trimmed = draft.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.replaceFirstChar { it.uppercaseChar() }
}
