package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionRef

/** the mention reference of the record this card or form is about */
fun EntityRef.toMentionRef(): MentionRef =
    when (this) {
        is EntityRef.Npc -> MentionRef(EntityKind.NPC, id)
        is EntityRef.Location -> MentionRef(EntityKind.LOCATION, id)
        is EntityRef.Quest -> MentionRef(EntityKind.QUEST, id)
        is EntityRef.Party -> MentionRef(EntityKind.PARTY, id)
        is EntityRef.Species -> MentionRef(EntityKind.SPECIES, id)
    }
