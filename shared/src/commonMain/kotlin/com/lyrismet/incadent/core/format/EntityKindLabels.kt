package com.lyrismet.incadent.core.format

import com.lyrismet.incadent.domain.model.EntityKind
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_party
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_species
import org.jetbrains.compose.resources.StringResource

/** the short kind name shown in the mention sheets and card overlines - NPC, Квест, Место, Отряд */
fun EntityKind.kindLabelRes(): StringResource =
    when (this) {
        EntityKind.NPC -> Res.string.session_detail_mention_type_npc
        EntityKind.PARTY -> Res.string.session_detail_mention_type_party
        EntityKind.QUEST -> Res.string.session_detail_mention_type_quest
        EntityKind.LOCATION -> Res.string.session_detail_mention_type_location
        EntityKind.SPECIES -> Res.string.session_detail_mention_type_species
    }
