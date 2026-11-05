package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.core.format.joinWithDot

/** a character class of the SRD 5.1, only the name */
data class SrdClass(
    val nameRu: String,
    val nameEn: String,
) {
    fun name(russian: Boolean): String = if (russian) nameRu else nameEn

    /** true when [name] is this class in either language */
    fun isNamed(name: String): Boolean {
        val typed = name.trim().foldForSearch()
        return typed.isNotEmpty() && (typed == nameRu.foldForSearch() || typed == nameEn.foldForSearch())
    }
}

/** the twelve classes the class sheet offers, in no particular order */
object SrdClasses {
    val all: List<SrdClass> =
        listOf(
            SrdClass("Бард", "Bard"),
            SrdClass("Варвар", "Barbarian"),
            SrdClass("Воин", "Fighter"),
            SrdClass("Волшебник", "Wizard"),
            SrdClass("Друид", "Druid"),
            SrdClass("Жрец", "Cleric"),
            SrdClass("Колдун", "Warlock"),
            SrdClass("Монах", "Monk"),
            SrdClass("Паладин", "Paladin"),
            SrdClass("Плут", "Rogue"),
            SrdClass("Следопыт", "Ranger"),
            SrdClass("Чародей", "Sorcerer"),
        )
}

/** the SRD classes whose name holds what is typed in either language, all of them while nothing is typed */
fun srdClassesMatching(
    query: String,
    russian: Boolean,
): List<SrdClass> {
    val typed = query.trim().foldForSearch()
    return SrdClasses.all
        .filter { typed.isEmpty() || typed in it.nameRu.foldForSearch() || typed in it.nameEn.foldForSearch() }
        .sortedBy { it.name(russian) }
}

/** true when [name] is a class of the SRD, in either language */
fun isSrdClass(name: String): Boolean = SrdClasses.all.any { it.isNamed(name) }

/** "Паладин · Клятва преданности", only the filled parts */
fun classLine(
    characterClass: String,
    subclass: String,
): String = joinWithDot(characterClass, subclass)
