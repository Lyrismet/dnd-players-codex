package com.lyrismet.incadent.domain.model

data class Npc(
    val id: Long,
    val name: String,
    val status: NpcStatus,
    val lifeState: NpcLifeState,
    val description: String,
    val locationId: Long?,
    val race: String,
    val faction: String,
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
