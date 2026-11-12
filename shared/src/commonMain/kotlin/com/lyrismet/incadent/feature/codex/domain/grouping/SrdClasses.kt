package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.core.format.joinWithDot

/** a subclass the class sheet offers as a one-press fill */
data class SrdSubclass(
    val nameRu: String,
    val nameEn: String,
) {
    fun name(russian: Boolean): String = if (russian) nameRu else nameEn
}

/** a character class of the SRD 5.1 with the subclasses it offers */
data class SrdClass(
    val nameRu: String,
    val nameEn: String,
    // the SRD 5.1 describes one per class, more can be listed here and the class sheet offers all of them
    val subclasses: List<SrdSubclass>,
) {
    fun subclassNames(russian: Boolean): List<String> = subclasses.map { it.name(russian) }

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
            SrdClass("Бард", "Bard", listOf(SrdSubclass("Коллегия знаний", "College of Lore"))),
            SrdClass("Варвар", "Barbarian", listOf(SrdSubclass("Путь берсерка", "Path of the Berserker"))),
            SrdClass("Воин", "Fighter", listOf(SrdSubclass("Чемпион", "Champion"))),
            SrdClass("Волшебник", "Wizard", listOf(SrdSubclass("Школа Воплощения", "School of Evocation"))),
            SrdClass("Друид", "Druid", listOf(SrdSubclass("Круг земли", "Circle of the Land"))),
            SrdClass("Жрец", "Cleric", listOf(SrdSubclass("Домен жизни", "Life Domain"))),
            SrdClass("Колдун", "Warlock", listOf(SrdSubclass("Исчадие", "The Fiend"))),
            SrdClass("Монах", "Monk", listOf(SrdSubclass("Путь открытой ладони", "Way of the Open Hand"))),
            SrdClass("Паладин", "Paladin", listOf(SrdSubclass("Клятва преданности", "Oath of Devotion"))),
            SrdClass("Плут", "Rogue", listOf(SrdSubclass("Вор", "Thief"))),
            SrdClass("Следопыт", "Ranger", listOf(SrdSubclass("Охотник", "Hunter"))),
            SrdClass("Чародей", "Sorcerer", listOf(SrdSubclass("Наследие драконьей крови", "Draconic Bloodline"))),
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

/** the SRD class [name] stands for, in either language */
fun srdClassNamed(name: String): SrdClass? = SrdClasses.all.firstOrNull { it.isNamed(name) }

/** true when [name] is a class of the SRD, in either language */
fun isSrdClass(name: String): Boolean = SrdClasses.all.any { it.isNamed(name) }

/** "Паладин · Клятва преданности", only the filled parts */
fun classLine(
    characterClass: String,
    subclass: String,
): String = joinWithDot(characterClass, subclass)
