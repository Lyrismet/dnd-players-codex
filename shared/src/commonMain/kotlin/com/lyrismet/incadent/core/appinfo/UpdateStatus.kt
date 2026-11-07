package com.lyrismet.incadent.core.appinfo

/** how the installed version compares to the latest published one */
sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus

    data class Available(
        val latest: VersionNumber,
    ) : UpdateStatus

    /** the latest version is not known, so nothing is offered */
    data object Unknown : UpdateStatus
}
