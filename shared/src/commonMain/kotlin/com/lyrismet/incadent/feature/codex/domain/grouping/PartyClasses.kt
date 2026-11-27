package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges

/** the classes a sheet or a form lists: the stored list, else the single class of the summary, else none */
fun PartyMember.classEntries(): List<PartyClass> = classEntriesOf(classes, characterClass, subclass, level)

/** [classEntries] for any holder of the three summary fields and the list, a form or a draft */
fun classEntriesOf(
    classes: List<PartyClass>,
    characterClass: String,
    subclass: String,
    level: Int?,
): List<PartyClass> =
    when {
        classes.isNotEmpty() -> classes
        characterClass.isNotBlank() -> listOf(PartyClass(characterClass, subclass, (level ?: 1).coerceAtLeast(1)))
        else -> emptyList()
    }

/** the sum of the levels, what the character level is while it has several classes */
fun List<PartyClass>.totalLevel(): Int = sumOf { it.level }

/** the levels a class at [index] (or a new one when null) can still take before the sum passes the top level */
fun List<PartyClass>.levelRoom(index: Int? = null): Int =
    PartyStatRanges.level.last - withIndex().filter { it.index != index }.sumOf { it.value.level }

/** true when [name] is already one of the classes, in either language, [exceptIndex] is the one being edited */
fun List<PartyClass>.hasClass(
    name: String,
    exceptIndex: Int? = null,
): Boolean {
    val typed = name.trim().foldForSearch()
    return typed.isNotEmpty() &&
        withIndex().any { (index, entry) ->
            index != exceptIndex &&
                (entry.characterClass.trim().foldForSearch() == typed || sameSrdClass(entry.characterClass, name))
        }
}

private fun sameSrdClass(
    first: String,
    second: String,
): Boolean = srdClassNamed(first)?.let { it == srdClassNamed(second) } == true

/** the summary fields a list of classes leaves on a record, the first class and the level sum */
data class ClassSummary(
    val characterClass: String,
    val subclass: String,
    val level: Int?,
)

/** the summary of [this] list, nothing for an empty one */
fun List<PartyClass>.summary(): ClassSummary? =
    firstOrNull()?.let {
        ClassSummary(
            it.characterClass,
            it.subclass,
            if (size ==
                1
            ) {
                it.level
            } else {
                totalLevel().coerceIn(PartyStatRanges.level)
            },
        )
    }

/** the list a record keeps in its column: two classes or more, one class lives in the summary fields alone */
fun List<PartyClass>.kept(): List<PartyClass> = if (size >= 2) this else emptyList()

/**
 * the member holding [entries]: two or more are kept as the list with the summary fields set from the first class,
 * one class goes back to the summary fields alone, none clears the class. The player's own character takes the level
 * sum, any other member keeps its level and its classes carry no level of their own
 */
fun PartyMember.withClasses(entries: List<PartyClass>): PartyMember {
    val summary = entries.summary() ?: ClassSummary("", "", level)
    return copy(
        classes = if (isPlayerCharacter) entries.kept() else entries.map { it.copy(level = 1) }.kept(),
        characterClass = summary.characterClass,
        subclass = summary.subclass,
        level = if (isPlayerCharacter || entries.isEmpty()) summary.level else level,
    )
}

/** the member with its summary fields in step with a stored list, what a write runs through */
fun PartyMember.withClassSummary(): PartyMember =
    when {
        classes.size < 2 -> copy(classes = emptyList())
        isPlayerCharacter -> withClasses(classes)
        else -> this
    }
