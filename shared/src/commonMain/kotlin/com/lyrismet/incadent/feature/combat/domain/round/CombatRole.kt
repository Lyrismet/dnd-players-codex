package com.lyrismet.incadent.feature.combat.domain.round

private const val SEPARATOR = " · "
private const val ELLIPSIS = "…"

/** what a participant is, told as kind, class and subclass, any of them can be empty */
data class RoleParts(
    val kind: String = "",
    val characterClass: String = "",
    val subclass: String = "",
) {
    fun text(): String = listOf(kind, characterClass, subclass).filter { it.isNotBlank() }.joinToString(SEPARATOR)

    companion object {
        /** the role typed for a participant that is only in the fight: class and subclass in one field */
        fun ofStored(role: String): RoleParts {
            val parts = role.split(SEPARATOR.trim()).map { it.trim() }.filter { it.isNotEmpty() }
            return RoleParts(
                characterClass = parts.firstOrNull().orEmpty(),
                subclass = parts.drop(1).joinToString(SEPARATOR),
            )
        }

        fun toStored(
            characterClass: String,
            subclass: String,
        ): String =
            listOf(characterClass, subclass).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(SEPARATOR)
    }
}

/**
 * the role line that fits the width [fits] measures: the kind goes first, then the end of the subclass is cut
 * with an ellipsis, the class is never cut
 */
fun fitRole(
    parts: RoleParts,
    fits: (String) -> Boolean,
): String {
    val rest = parts.copy(kind = "")
    val hasBody = rest.text().isNotEmpty()
    val prefix = if (parts.characterClass.isBlank()) "" else parts.characterClass + SEPARATOR
    val candidates =
        sequence {
            yield(parts.text())
            if (parts.kind.isNotBlank() && hasBody) yield(rest.text())
            for (keep in parts.subclass.length - 1 downTo 1) {
                yield(prefix + parts.subclass.take(keep).trimEnd() + ELLIPSIS)
            }
        }
    val last = if (hasBody) parts.characterClass.ifBlank { parts.subclass } else parts.text()
    return candidates.firstOrNull(fits) ?: last
}
