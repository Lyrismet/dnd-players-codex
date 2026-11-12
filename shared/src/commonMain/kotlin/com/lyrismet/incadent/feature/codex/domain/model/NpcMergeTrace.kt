package com.lyrismet.incadent.feature.codex.domain.model

/** the kinds of record whose text can carry an "@" token of an npc */
enum class MentionHost { SPECIES, PLACE, NPC, QUEST, PARTY, ITEM, ENTRY }

/** a text as it was before the merge moved the mentions in it */
data class MergedText(
    val host: MentionHost,
    val id: Long,
    val before: String,
)

/** what a merge of two npcs changed, enough to bring the record that went away back with everything it held */
data class NpcMergeTrace(
    val survivorBefore: Npc,
    val loser: Npc,
    val combatantIds: List<Long>,
    val questIds: List<Long>,
    val texts: List<MergedText>,
    // the origin ids that led to the loser before the merge, they led nowhere else after it
    val repointedAliases: List<String>,
)
