package com.lyrismet.incadent.domain.model

import kotlinx.datetime.LocalDateTime

enum class CampaignStatus { LIVE, ARCHIVED }

/** what the campaign library card shows - the player character is the "вы" member of the party, if there is one */
data class Campaign(
    val id: String,
    val name: String,
    val status: CampaignStatus,
    val sessionCount: Int,
    val lastSessionDate: LocalDateTime?,
    val player: CampaignPlayer?,
)

data class CampaignPlayer(
    val name: String,
    val characterClass: String,
    val level: Int?,
)

/** a place or npc of another campaign that can be copied into a new one, [subtitle] tells same-named ones apart */
data class TransferCandidate(
    val id: Long,
    val name: String,
    val subtitle: String,
)

data class TransferCandidates(
    val places: List<TransferCandidate>,
    val npcs: List<TransferCandidate>,
)

/** what a new campaign takes from the source one, a place's parents and an npc's places are copied with it */
data class CampaignTransfer(
    val sourceId: String,
    val placeIds: Set<Long> = emptySet(),
    val npcIds: Set<Long> = emptySet(),
    val copyTags: Boolean = false,
    val copyPhrases: Boolean = false,
)
