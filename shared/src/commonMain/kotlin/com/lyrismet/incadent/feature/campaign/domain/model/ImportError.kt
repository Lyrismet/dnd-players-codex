package com.lyrismet.incadent.feature.campaign.domain.model

/** why a file cannot be imported, each one has its own message and all of them offer another file */
sealed interface ImportError {
    data object NotIncadent : ImportError

    data object TooLarge : ImportError

    /** the file carries the mark but its content does not hold together */
    data object Malformed : ImportError
}

class ImportFailedException(
    val error: ImportError,
) : Exception("import failed: $error")

/** the biggest file taken, an export warns past it and an import checks it against the size the system reports */
const val MAX_FILE_BYTES = 25L * 1024 * 1024
