package com.lyrismet.incadent.feature.campaign.domain.exchange

const val EXPORT_FILE_EXTENSION = ".incadent.json"

private const val MAX_BASE_CHARS = 60
private const val FALLBACK_BASE = "incadent"

/**
 * the file name an export gets: the campaign name with every character a file system may refuse turned into "_",
 * and the [label] of the mode ("personal copy", "for a friend") after a dot
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
            ?.let { " · $it" }
            .orEmpty()
    return base.ifEmpty { FALLBACK_BASE } + tail + EXPORT_FILE_EXTENSION
}

private fun String.safeName(): String =
    map { if (it.isSafeInName()) it else '_' }
        .joinToString("")
        .trim()
        .trim('_', '.')
        .trim()

private fun Char.isSafeInName(): Boolean = isLetterOrDigit() || this in " -_"
