package com.lyrismet.incadent.core.appinfo

/** the installed app version, [name] is shown to people and [code] orders builds */
data class AppVersion(
    val name: String,
    val code: Int,
) {
    companion object {
        private const val UNKNOWN_NAME = "0.0.0"

        /** builds a version from what the platform reports, a missing or blank value falls back to a zero version */
        fun of(
            rawName: String?,
            rawCode: String?,
        ): AppVersion =
            AppVersion(
                name = rawName?.trim().orEmpty().ifEmpty { UNKNOWN_NAME },
                code = rawCode?.trim()?.toIntOrNull() ?: 0,
            )
    }
}

/** reads the version the platform build was stamped with */
expect class AppVersionReader {
    fun read(): AppVersion
}
