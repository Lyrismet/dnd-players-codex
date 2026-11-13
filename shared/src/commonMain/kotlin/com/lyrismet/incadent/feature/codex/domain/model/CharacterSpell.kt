package com.lyrismet.incadent.feature.codex.domain.model

/** a spell of a character, level 0 is a cantrip, [freeMax] casts a rest can bring back are paid without a slot */
data class CharacterSpell(
    val id: Long,
    val name: String,
    val level: Int,
    val school: String = "",
    val castingTime: String = "",
    val range: String = "",
    val verbal: Boolean = false,
    val somatic: Boolean = false,
    val hasMaterial: Boolean = false,
    val material: String = "",
    val duration: String = "",
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val text: String = "",
    val freeMax: Int = 0,
    val freeLeft: Int = 0,
    val position: Int = 0,
    val originId: String = "",
) {
    val isCantrip: Boolean get() = level == CANTRIP_LEVEL
    val hasFreeCasts: Boolean get() = freeMax > 0

    /** the components as the card writes them: "В, С, М (a pinch of sand)", empty when none is set */
    val hasComponents: Boolean get() = verbal || somatic || hasMaterial

    fun clamped(): CharacterSpell {
        val cap = freeMax.coerceIn(FREE_CASTS_RANGE)
        return copy(
            name = name.trim(),
            level = level.coerceIn(CANTRIP_LEVEL, SPELL_LEVELS.last),
            freeMax = cap,
            freeLeft = freeLeft.coerceIn(0, cap),
        )
    }
}

const val CANTRIP_LEVEL = 0
val FREE_CASTS_RANGE = 0..9
val SPELL_POSITIONS = 0..9_999
