package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDown

/** one thing the results can write into the codex, shown as a line with a tick */
sealed interface CodexItem {
    val key: String

    data class HitObserved(
        val draft: HitObservationDraft,
    ) : CodexItem {
        override val key get() = "hits-${draft.memberId}"
    }

    /** a named enemy who went down is marked dead */
    data class MarkDead(
        val npcId: Long,
        val name: String,
    ) : CodexItem {
        override val key get() = "dead-$npcId"
    }

    /** a named ally who went down is marked fallen */
    data class MarkFallen(
        val npcId: Long,
        val name: String,
    ) : CodexItem {
        override val key get() = "fallen-$npcId"
    }

    /** a party member who did not come back from the death saves leaves the party */
    data class MarkPartyFallen(
        val partyMemberId: Long,
        val name: String,
    ) : CodexItem {
        override val key get() = "party-fallen-$partyMemberId"
    }

    data class Resistance(
        val guess: ResistanceGuess,
    ) : CodexItem {
        override val key get() = "res-${guess.speciesId}-${guess.type.name}"
    }

    /** a temporary ally the master may keep as a named npc friend */
    data class KeepAlly(
        val memberId: Long,
        val name: String,
    ) : CodexItem {
        override val key get() = "keep-$memberId"
    }
}

data class CodexEntry(
    val item: CodexItem,
    val checked: Boolean,
)

/** the proposals with the ticks they start with, only what is ticked is written */
fun codexChecklist(
    combat: Combat,
    species: Map<Long, Species>,
): List<CodexEntry> {
    val hits = hitObservations(combat).map { CodexEntry(CodexItem.HitObserved(it), true) }
    val marks = combat.combatants.mapNotNull(::markOf)
    val fallen =
        combat.combatants
            .filter { it.side == CombatSide.PARTY && it.dead && it.partyMemberId != null }
            .map { CodexEntry(CodexItem.MarkPartyFallen(checkNotNull(it.partyMemberId), it.name), true) }
    val guesses = inferResistances(combat, species).map { CodexEntry(CodexItem.Resistance(it), it.confident) }
    val keep =
        combat.combatants
            .filter { it.side == CombatSide.ALLY && it.npcId == null && it.speciesId == null && !it.isDown() }
            .map { CodexEntry(CodexItem.KeepAlly(it.id, it.name), false) }
    return hits + marks + fallen + guesses + keep
}

private fun markOf(member: Combatant): CodexEntry? {
    val npcId = member.npcId
    return when {
        npcId == null || !member.isDown() || member.captured -> null
        member.side == CombatSide.ENEMY -> CodexEntry(CodexItem.MarkDead(npcId, member.name), true)
        member.side == CombatSide.ALLY -> CodexEntry(CodexItem.MarkFallen(npcId, member.name), true)
        else -> null
    }
}
