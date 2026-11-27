package com.lyrismet.incadent.feature.codex.presentation.form

/** the class and subclass a record has now, the class sheet starts from them, [level] is set only for a party member */
data class ClassSelectState(
    val characterClass: String,
    val subclass: String,
    val level: ClassLevelState? = null,
)

/** the level step of a class sheet, where it starts, the most it takes, the classes listed and whether it can go */
data class ClassLevelState(
    val level: Int,
    val max: Int,
    val taken: List<String> = emptyList(),
    val canRemove: Boolean = false,
    val showLevel: Boolean = true,
)
