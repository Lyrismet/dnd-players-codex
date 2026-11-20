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

/** one edit of the own shield list, a tap that walks the state on or a state picked by hand */
internal sealed interface ShieldChange {
    val type: DamageType

    fun applyTo(own: Map<DamageType, DamageState>): Map<DamageType, DamageState>

    /** a tap walks the state on through resistance, immunity, vulnerability and none */
    data class Step(
        override val type: DamageType,
        val inherited: Map<DamageType, DamageState>,
    ) : ShieldChange {
        override fun applyTo(own: Map<DamageType, DamageState>) =
            own.with(type, nextOwnVulnerability(inherited, own, type))
    }

    data class Set(
        override val type: DamageType,
        val state: DamageState?,
    ) : ShieldChange {
        override fun applyTo(own: Map<DamageType, DamageState>) = own.with(type, state)
    }
}

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

    fun change(
        memberId: Long?,
        change: ShieldChange,
    ) {
        editor.withMember(memberId ?: return) { _, member ->
            learner.writeOwn(member, change.applyTo(learner.ownShields(member)))
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
