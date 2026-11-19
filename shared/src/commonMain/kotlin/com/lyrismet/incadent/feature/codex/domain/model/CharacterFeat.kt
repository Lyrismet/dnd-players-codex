package com.lyrismet.incadent.feature.codex.domain.model

/** where a feat of a character comes from, an unset source is just a thing the character can do */
enum class FeatSource {
    FEAT,
    RACE,
    CLASS,
    BACKGROUND,
    OTHER,
}

/** a named thing a character can do, [description] says what it gives */
data class CharacterFeat(
    val id: Long = 0,
    val name: String,
    val source: FeatSource? = null,
    val description: String = "",
    val position: Int = 0,
    val originId: String = "",
)

const val FEAT_NAME_MAX = 80
