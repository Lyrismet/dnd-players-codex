package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.Photo

data class Npc(
    val id: Long,
    val name: String,
    val status: NpcStatus,
    val lifeState: NpcLifeState,
    val description: String,
    // every place the npc can be met at, in no particular order
    val locationIds: List<Long>,
    val race: String,
    val portrait: Photo? = null,
    val nicknames: List<String> = emptyList(),
    // the kind of creature the npc is, null when it is none of the known species
    val speciesId: Long? = null,
    // only what the npc has of its own, the rest is read from the species
    val vulnerabilities: Map<DamageType, DamageState> = emptyMap(),
)

/** the party's relation to this npc - independent of whether the npc is still alive */
enum class NpcStatus {
    FRIEND,
    ENEMY,
    NEUTRAL,
}

enum class NpcLifeState {
    ALIVE,
    DEAD,
}
