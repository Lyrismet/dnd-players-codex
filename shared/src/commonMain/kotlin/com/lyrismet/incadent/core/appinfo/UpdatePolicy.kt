package com.lyrismet.incadent.core.appinfo

/** the smallest gap between installed and latest that is worth offering an update for */
enum class UpdateThreshold {
    ANY,
    MINOR,
    MAJOR,
}

class UpdatePolicy(
    private val threshold: UpdateThreshold = UpdateThreshold.ANY,
) {
    fun isWorthOffering(
        installed: VersionNumber,
        latest: VersionNumber,
    ): Boolean =
        latest > installed &&
            when (threshold) {
                UpdateThreshold.ANY -> true
                UpdateThreshold.MINOR -> latest.major > installed.major || latest.minor > installed.minor
                UpdateThreshold.MAJOR -> latest.major > installed.major
            }

    /** a missing or unreadable version on either side gives [UpdateStatus.Unknown] */
    fun statusOf(
        installed: VersionNumber?,
        latest: VersionNumber?,
    ): UpdateStatus =
        when {
            installed == null || latest == null -> UpdateStatus.Unknown
            isWorthOffering(installed, latest) -> UpdateStatus.Available(latest)
            else -> UpdateStatus.UpToDate
        }
}
