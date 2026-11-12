package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** how a participant stands to the codex: a character of the party, a creature still unnamed, a record or a label */
internal enum class MemberTie {
    PARTY,

    // a creature of a species that still carries the species name
    UNNAMED,
    CODEX,

    // a name given for this fight only, or a participant made by hand
    LABELLED,
}

/** told by the record and the flag a participant carries, never by what it is called */
internal fun Combatant.tie(): MemberTie =
    when {
        side == CombatSide.PARTY -> MemberTie.PARTY
        npcId != null -> MemberTie.CODEX
        isLabel || speciesId == null -> MemberTie.LABELLED
        else -> MemberTie.UNNAMED
    }

/** a participant of the codex has its link mark, a label says it is only here, the rest have none */
internal fun MemberTie.link(): RowLink =
    when (this) {
        MemberTie.CODEX -> RowLink.CODEX
        MemberTie.LABELLED -> RowLink.BATTLE_ONLY
        else -> RowLink.NONE
    }
