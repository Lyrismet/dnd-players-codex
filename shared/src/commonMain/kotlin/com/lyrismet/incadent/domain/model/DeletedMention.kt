package com.lyrismet.incadent.domain.model

/** what kind of record a deleted mention used to point at */
enum class DeletedMentionType { NPC, PARTY, LOCATION, QUEST }

/** a codex record that is gone while notes still mention it - the name is the one its token carried, shown as "?" */
data class DeletedMention(
    val id: Long,
    val name: String,
    val type: DeletedMentionType,
)
