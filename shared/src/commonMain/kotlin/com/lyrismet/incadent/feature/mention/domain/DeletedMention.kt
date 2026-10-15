package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind

/** a codex record that is gone while notes still mention it - the name is the one its token carried, shown as "?" */
data class DeletedMention(
    val id: Long,
    val name: String,
    val type: EntityKind,
)
