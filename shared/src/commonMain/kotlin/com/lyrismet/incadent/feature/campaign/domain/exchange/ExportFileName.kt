package com.lyrismet.incadent.feature.campaign.domain.exchange

const val EXPORT_FILE_EXTENSION = ".incadent.json"

private const val MAX_BASE_CHARS = 60
private const val FALLBACK_BASE = "incadent"

/** the file name an export gets: the campaign name with every character a file system may refuse turned into "_" */
fun exportFileName(campaignName: String): String {
    val base =
        campaignName
            .map { if (it.isSafeInName()) it else '_' }
            .joinToString("")
            .trim()
            .trim('_', '.')
            .take(MAX_BASE_CHARS)
            .trim()
    return base.ifEmpty { FALLBACK_BASE } + EXPORT_FILE_EXTENSION
}

private fun Char.isSafeInName(): Boolean = isLetterOrDigit() || this in " -_"
