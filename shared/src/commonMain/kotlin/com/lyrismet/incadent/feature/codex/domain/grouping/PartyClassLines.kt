package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember

/** "Жрец - Домен Света", the class with its subclass when there is one */
fun partyClassLine(
    characterClass: String,
    subclass: String,
): String = if (subclass.isBlank()) characterClass else "$characterClass - $subclass"

/** "Жрец - Домен Света 3 ур. / Воин 2 ур.", [levelTemplate] has "{level}" where the number goes */
fun List<PartyClass>.classesLine(levelTemplate: String): String =
    joinToString(" / ") {
        "${partyClassLine(it.characterClass, it.subclass)} ${levelTemplate.replace("{level}", it.level.toString())}"
    }

/** "Жрец / Воин", only the class names */
fun List<PartyClass>.classNamesLine(): String = joinToString(" / ") { it.characterClass }

/** the class names over a card, all of them while the member has several */
fun PartyMember.classOverline(): String = if (classes.size >= 2) classes.classNamesLine() else characterClass

/** the class row of a card, every class with its level while the member has several */
fun PartyMember.classFact(levelTemplate: String): String =
    if (classes.size >= 2) classes.classesLine(levelTemplate) else classLine(characterClass, subclass)
