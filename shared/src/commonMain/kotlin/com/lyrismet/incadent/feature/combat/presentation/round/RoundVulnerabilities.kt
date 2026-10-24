package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.grouping.effectiveVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_recorded_sub
import dndplayerscodex.shared.generated.resources.combat_calc_recorded_title
import org.jetbrains.compose.resources.getString

/** writes a state picked in the panel to the card of the chosen participant, its npc or else its species */
@Suppress("LongParameterList")
internal fun recordVulnerability(
    editor: RoundEditor,
    learner: VulnerabilityLearner,
    event: CombatRoundEvent.VulnerabilityChanged,
    memberId: Long?,
    species: Species?,
    words: RoundWords,
) {
    editor.withMember(memberId ?: return) { _, member ->
        if (species == null && member.npcId == null) return@withMember
        val note =
            UndoNote(
                getString(
                    Res.string.combat_calc_recorded_title,
                    words.stateTitles.getValue(event.state),
                    words.typeNames.getValue(event.type),
                ),
                getString(Res.string.combat_calc_recorded_sub, member.name),
            )
        learner.record(editor.combatId, member, species?.id, event.type, event.state, note)
    }
}

/** what each participant is vulnerable to: its species' list with the entries of its own npc card on top */
internal fun resistancesOf(
    ordered: List<Combatant>,
    npcs: List<Npc>,
    speciesOf: Map<Long, Species>,
): Map<Long, Map<DamageType, DamageState>> =
    ordered
        .mapNotNull { member ->
            val own =
                member.npcId
                    ?.let { id -> npcs.firstOrNull { it.id == id } }
                    ?.vulnerabilities
                    .orEmpty()
            val inherited = speciesOf[member.id]?.vulnerabilities.orEmpty()
            if (own.isEmpty() && inherited.isEmpty()) null else member.id to effectiveVulnerabilities(inherited, own)
        }.toMap()
