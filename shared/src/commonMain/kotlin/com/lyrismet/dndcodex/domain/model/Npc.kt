package com.lyrismet.dndcodex.domain.model

data class Npc(
    val id: Long,
    val name: String,
    val status: NpcStatus,
    val description: String,
    val locationId: Long?,
)

enum class NpcStatus {
    FRIEND,
    ENEMY,
    NEUTRAL,
    DEAD,
}
