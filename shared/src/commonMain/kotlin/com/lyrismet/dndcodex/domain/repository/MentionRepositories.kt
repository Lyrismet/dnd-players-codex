package com.lyrismet.dndcodex.domain.repository

/** the three codex lookups every @mention feature needs - npc/location/quest names, statuses and ids */
data class MentionRepositories(
    val npcRepository: NpcRepository,
    val locationRepository: LocationRepository,
    val questRepository: QuestRepository,
)
