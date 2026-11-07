package com.lyrismet.incadent.core.appinfo

/** a major.minor.patch version, ordered by major first and patch last */
data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<VersionNumber> {
    override fun compareTo(other: VersionNumber): Int =
        compareValuesBy(this, other, VersionNumber::major, VersionNumber::minor, VersionNumber::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private const val PARTS = 3

        /** parses "0.8.1", returns null for anything that is not three non-negative numbers */
        fun parse(raw: String): VersionNumber? {
            val parts = raw.trim().split('.')
            if (parts.size != PARTS) return null
            val numbers = parts.map { part -> part.takeIf { it.all(Char::isDigit) }?.toIntOrNull() }
            val (major, minor, patch) = numbers
            return if (major == null || minor == null || patch == null) null else VersionNumber(major, minor, patch)
        }
    }
}
