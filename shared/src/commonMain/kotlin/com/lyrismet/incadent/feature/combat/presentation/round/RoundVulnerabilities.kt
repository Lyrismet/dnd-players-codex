package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.codex.domain.grouping.effectiveVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.grouping.nextOwnVulnerability
import com.lyrismet.incadent.feature.codex.domain.grouping.with
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_features_changed
import org.jetbrains.compose.resources.getString

/** the own list a participant had when the editing of its shields began, to take the whole editing back at once */
internal data class ShieldEditDraft(
    val memberId: Long,
    val before: Map<DamageType, DamageState>,
)

/** the shields of the chosen participant: each tap writes quietly, the one undo bar comes when the editing is done */
internal class ShieldEditing(
    private val editor: RoundEditor,
    private val learner: VulnerabilityLearner,
    private val draft: MutableState<ShieldEditDraft?>,
) {
    fun start(memberId: Long?) {
        if (draft.value != null) return
        editor.withMember(memberId ?: return) { _, member ->
            draft.value = ShieldEditDraft(member.id, learner.ownShields(member))
        }
    }

    /** a tap on a shield walks its state on: resistance, immunity, vulnerability, none */
    fun tap(
        memberId: Long?,
        type: DamageType,
        inherited: Map<DamageType, DamageState>,
    ) = change(memberId) { own -> own.with(type, nextOwnVulnerability(inherited, own, type)) }

    fun set(
        memberId: Long?,
        type: DamageType,
        state: DamageState?,
    ) = change(memberId) { own -> own.with(type, state) }

    private fun change(
        memberId: Long?,
        next: (Map<DamageType, DamageState>) -> Map<DamageType, DamageState>,
    ) {
        editor.withMember(memberId ?: return) { _, member ->
            learner.writeOwn(member, next(learner.ownShields(member)))
        }
    }

    fun done() {
        val started = draft.value ?: return
        draft.value = null
        editor.withMember(started.memberId) { combat, member ->
            if (learner.ownShields(member) == started.before) return@withMember
            learner.offerUndo(
                UndoNote(getString(Res.string.combat_features_changed), member.name),
            ) {
                combat.combatants.firstOrNull { it.id == member.id }?.let { learner.writeOwn(it, started.before) }
            }
        }
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
                member.vulnerabilities +
                    member.npcId
                        ?.let { id -> npcs.firstOrNull { it.id == id } }
                        ?.vulnerabilities
                        .orEmpty()
            val inherited = speciesOf[member.id]?.vulnerabilities.orEmpty()
            if (own.isEmpty() && inherited.isEmpty()) null else member.id to effectiveVulnerabilities(inherited, own)
        }.toMap()
