package com.lyrismet.incadent.core.appinfo

/** a major.minor.patch version with an optional fourth revision number, ordered from major down to revision */
data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val revision: Int = 0,
) : Comparable<VersionNumber> {
    override fun compareTo(other: VersionNumber): Int =
        compareValuesBy(
            this,
            other,
            VersionNumber::major,
            VersionNumber::minor,
            VersionNumber::patch,
            VersionNumber::revision,
        )

    override fun toString(): String = "$major.$minor.$patch" + if (revision == 0) "" else ".$revision"

    companion object {
        private val PART_COUNTS = 3..4
        private const val REVISION_INDEX = 3

        /** parses "0.8.1" or "0.9.0.1", returns null for anything that is not three or four non-negative numbers */
        fun parse(raw: String): VersionNumber? {
            val parts = raw.trim().split('.')
            val numbers = parts.map { part -> part.takeIf { it.all(Char::isDigit) }?.toIntOrNull() }
            val valid = parts.size in PART_COUNTS && numbers.all { it != null }
            return if (valid) {
                val (major, minor, patch) = numbers.filterNotNull()
                VersionNumber(major, minor, patch, numbers.getOrNull(REVISION_INDEX) ?: 0)
            } else {
                null
            }
        }
    }
}
