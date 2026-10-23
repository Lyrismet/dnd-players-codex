package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.Photo

/** a kind of creature the party meets (goblins, wolves), as opposed to a named npc */
data class Species(
    val id: Long,
    val name: String,
    val creatureType: CreatureType,
    // only the types that are known, a type missing here is [DamageState.UNKNOWN]
    val vulnerabilities: Map<DamageType, DamageState> = emptyMap(),
    val notes: String = "",
    val photo: Photo? = null,
    val nicknames: List<String> = emptyList(),
    // known by the player from somewhere, not filled in by a fight - null while nobody knows it
    val hitPoints: Int? = null,
)

/** the hit points a species can be given */
val SPECIES_HIT_POINTS_RANGE = 1..999

/** the 5e creature types - the one list the form, the card and the list read */
enum class CreatureType {
    ABERRATION,
    BEAST,
    CELESTIAL,
    CONSTRUCT,
    DRAGON,
    ELEMENTAL,
    FEY,
    FIEND,
    GIANT,
    HUMANOID,
    MONSTROSITY,
    OOZE,
    PLANT,
    UNDEAD,

    /** a species made from a name alone, the card shows that the type is not set */
    UNSPECIFIED,
}

/** the 5e damage types, kept apart from the rules system so another preset can bring its own set later */
enum class DamageType {
    BLUDGEONING,
    PIERCING,
    SLASHING,
    FIRE,
    COLD,
    LIGHTNING,
    THUNDER,
    ACID,
    POISON,
    NECROTIC,
    RADIANT,
    FORCE,
    PSYCHIC,
}

enum class DamageState {
    UNKNOWN,
    NORMAL,
    RESISTANCE,
    IMMUNITY,
    VULNERABILITY,
}
