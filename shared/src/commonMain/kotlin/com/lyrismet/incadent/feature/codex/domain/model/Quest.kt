package com.lyrismet.incadent.feature.codex.domain.model

data class Quest(
    val id: Long,
    val title: String,
    val status: QuestStatus,
    val reward: String,
    val givenByNpcId: Long?,
    val locationId: Long?,
    val description: String = "",
    val nicknames: List<String> = emptyList(),
)

enum class QuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED,
}
