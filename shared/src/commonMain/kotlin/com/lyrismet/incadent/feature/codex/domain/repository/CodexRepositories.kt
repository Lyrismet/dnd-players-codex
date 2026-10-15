package com.lyrismet.incadent.feature.codex.domain.repository

/** the four codex stores every feature that lists, mentions or opens a record works with, injected as one */
data class CodexRepositories(
    val partyRepository: PartyRepository,
    val npcRepository: NpcRepository,
    val questRepository: QuestRepository,
    val locationRepository: LocationRepository,
)
