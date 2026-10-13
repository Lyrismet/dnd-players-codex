package com.lyrismet.incadent.domain.model

/** what kind of record a deleted mention used to point at */
enum class DeletedMentionType { NPC, PARTY, LOCATION, QUEST }

/** the name and kind of a codex record that was deleted while notes still mention it - kept so the mention reads "?" */
data class DeletedMention(
    val name: String,
    val type: DeletedMentionType,
)
