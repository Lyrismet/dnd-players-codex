package com.lyrismet.incadent.feature.campaign.domain.exchange

const val EXPORT_FILE_EXTENSION = ".incadent.json"

private const val MAX_BASE_CHARS = 60
private const val FALLBACK_BASE = "incadent"

/**
 * the file name an export gets: the campaign name with every character a file system may refuse turned into "_",
 * and the [label] of the mode ("personal copy", "for a friend") after an underscore, spaces become underscores too
 */
fun exportFileName(
    campaignName: String,
    label: String = "",
): String {
    val base = campaignName.safeName().take(MAX_BASE_CHARS).trim()
    val tail =
        label
            .safeName()
            .takeIf { it.isNotEmpty() }
            ?.let { "_$it" }
            .orEmpty()
    return base.ifEmpty { FALLBACK_BASE } + tail + EXPORT_FILE_EXTENSION
}

private const val JSON_EXTENSION = ".json"
private const val APP_EXTENSION = ".incadent"
const val MAX_TYPED_CHARS = 120

/** the name a person typed in the save sheet made safe for a file system, empty when nothing usable is left */
fun typedFileName(typed: String): String {
    val clean =
        typed
            .take(MAX_TYPED_CHARS)
            .map { if (it in "/\\:*?\"<>|" || it.isISOControl()) '_' else it }
            .joinToString("")
            .trim()
            .trim('.')
            .trim()
            .withoutSpaces()
    if (clean.isEmpty() || clean.all { it == '_' }) return ""
    return when {
        clean.endsWith(JSON_EXTENSION, ignoreCase = true) -> clean
        clean.endsWith(APP_EXTENSION, ignoreCase = true) -> clean + JSON_EXTENSION
        else -> clean + EXPORT_FILE_EXTENSION
    }
}

private fun String.safeName(): String =
    map { if (it.isSafeInName()) it else '_' }
        .joinToString("")
        .trim()
        .trim('_', '.')
        .trim()
        .withoutSpaces()

// some places refuse a space in a file name, a run of spaces and underscores becomes one underscore
private fun String.withoutSpaces(): String = replace(Regex("[ _]{2,}|\\s"), "_")

private fun Char.isSafeInName(): Boolean = isLetterOrDigit() || this in " -_"
