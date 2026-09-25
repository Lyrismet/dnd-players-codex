package com.lyrismet.dndcodex.domain.model

data class Quest(
    val id: Long,
    val title: String,
    val status: QuestStatus,
    val reward: String,
    val givenByNpcId: Long?,
    val locationId: Long?,
)

enum class QuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED,
}
