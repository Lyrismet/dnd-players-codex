package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.runtime.Immutable
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionRef

/** the [source] of a notes field and the record it is written in ([self]), left out of its own "@" list */
@Immutable
class MentionField(
    val source: MentionSource,
    val self: MentionRef?,
)

/** what a notes field of a record needs to offer "@": the records to pick and the way to make a new one */
@Immutable
class MentionSource(
    val entities: List<MentionEntity>,
    val lookups: MentionLookups,
    val create: suspend (kind: EntityKind, name: String) -> MentionRef,
)
