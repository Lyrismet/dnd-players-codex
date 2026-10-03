package com.lyrismet.incadent.domain.model

data class Npc(
    val id: Long,
    val name: String,
    val status: NpcStatus,
    val description: String,
    val locationId: Long?,
    val race: String,
    val faction: String,
)

enum class NpcStatus {
    FRIEND,
    ENEMY,
    NEUTRAL,
    DEAD,
}
