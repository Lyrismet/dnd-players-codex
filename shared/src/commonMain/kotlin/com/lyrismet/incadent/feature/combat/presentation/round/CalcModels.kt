package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.core.calc.CalcExpression
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.slack.circuit.runtime.CircuitUiEvent

enum class CalcMode { DAMAGE, HEAL, TEMP }

/** a blow already dealt in this calculator, shown above the targets while "another hit" keeps it open */
data class DoneHit(
    val label: String,
    val damage: Int,
)

/** what the calculator has been told so far, it lives in the presenter while the sheet is open */
internal data class CalcDraft(
    val mode: CalcMode,
    val targets: List<Long>,
    // the one who deals it, none stands for the environment
    val sourceId: Long?,
    val expr: CalcExpression = CalcExpression(),
    // the type typed for each term of the sum, by position
    val termTypes: List<DamageType?> = emptyList(),
    val currentType: DamageType? = null,
    val scales: Map<Long, HitScale> = emptyMap(),
    val selectedTarget: Long? = null,
    val hit: Int = 1,
    val done: List<DoneHit> = emptyList(),
    val collapsed: Boolean = false,
)

data class CalcState(
    val mode: CalcMode,
    val modes: List<CalcMode>,
    val overline: String,
    val title: String,
    val sources: List<SourceChip>,
    val sourceHint: String,
    val done: List<DoneHit>,
    val targetsHeader: String,
    val reaction: CalcReaction?,
    val targets: List<CalcTarget>,
    val offTargets: List<OffTarget>,
    // targets as rows with the keypad folded away, or as chips with the keypad open
    val rowMode: Boolean,
    val selectedTarget: CalcTarget?,
    val typeHeader: String,
    val typeLabel: String,
    val types: List<TypeChip>,
    val terms: List<TermChip>,
    val result: String,
    val preview: List<String>,
    val keysShown: Boolean,
    val canMore: Boolean,
    val applyLabel: String,
    val applyEnabled: Boolean,
)

data class SourceChip(
    val id: Long?,
    val label: String,
    val side: RowSide,
    val selected: Boolean,
)

data class CalcReaction(
    val available: Boolean,
    val label: String,
    val memberId: Long,
)

/** the shield of the first term a resistance changed, [type] gives its icon */
data class CalcShield(
    val type: DamageType?,
    val effect: HitEffect,
)

data class CalcPart(
    val prefix: String,
    val type: DamageType?,
    val text: String,
)

data class CalcTarget(
    val id: Long,
    val name: String,
    val side: RowSide,
    val scale: HitScale,
    val shield: CalcShield?,
    val result: String?,
    val parts: List<CalcPart>,
    val selected: Boolean,
    val scales: List<ScaleOption>,
    val quick: QuickResistance?,
)

data class ScaleOption(
    val scale: HitScale,
    val label: String,
    val selected: Boolean,
)

/** "it is a resistance: fire" for a species that does not yet know its answer to the type */
data class QuickResistance(
    val label: String,
    val type: DamageType,
    val state: DamageState,
)

data class OffTarget(
    val id: Long,
    val name: String,
)

data class TypeChip(
    val type: DamageType,
    val selected: Boolean,
)

data class TermChip(
    val text: String,
    val type: DamageType?,
    val current: Boolean,
)

sealed interface CalcEvent : CircuitUiEvent {
    data class Opened(
        val mode: CalcMode,
    ) : CalcEvent

    data object Closed : CalcEvent

    data class ModeSelected(
        val mode: CalcMode,
    ) : CalcEvent

    data class SourceSelected(
        val id: Long?,
    ) : CalcEvent

    data class Digit(
        val digit: Int,
    ) : CalcEvent

    data object Plus : CalcEvent

    data object Backspace : CalcEvent

    data object Clear : CalcEvent

    data object CollapseToggled : CalcEvent

    data class TypeSelected(
        val type: DamageType,
    ) : CalcEvent

    data class TargetToggled(
        val id: Long,
    ) : CalcEvent

    data class TargetRemoved(
        val id: Long,
    ) : CalcEvent

    data class TargetAdded(
        val id: Long,
    ) : CalcEvent

    data class ScaleSelected(
        val id: Long,
        val scale: HitScale,
    ) : CalcEvent

    data object QuickRecorded : CalcEvent

    data class ReactionToggled(
        val memberId: Long,
    ) : CalcEvent

    data object MoreHit : CalcEvent

    data object Applied : CalcEvent
}

/** the round summary: a sheet over the fight with the damage of one round, blow by blow */
data class SummaryState(
    val title: String,
    val rounds: List<SummaryRound>,
    val targets: List<SummaryTarget>,
    val recordLabel: String,
    val record: Boolean,
)

data class SummaryRound(
    val number: Int,
    val sum: String,
    val selected: Boolean,
)

data class SummaryTarget(
    val name: String,
    val side: RowSide,
    val sum: String,
    val state: String,
    val hits: List<SummaryHit>,
)

data class SummaryHit(
    val source: String,
    val chips: List<SummaryChip>,
    val scaleLabel: String?,
    val total: String,
    val popup: SummaryPopup?,
    val canRecord: Boolean,
)

data class SummaryChip(
    val eventId: Long,
    val type: DamageType?,
    val old: String?,
    val now: String,
    val effect: HitEffect,
    val changed: Boolean,
    val tappable: Boolean,
    val open: Boolean,
)

data class SummaryPopup(
    val title: String,
    val eventId: Long,
    val options: List<SummaryOption>,
)

data class SummaryOption(
    val effect: HitEffect,
    val label: String,
    val selected: Boolean,
)

sealed interface SummaryEvent : CircuitUiEvent {
    data object Opened : SummaryEvent

    data object Closed : SummaryEvent

    data class RoundSelected(
        val number: Int,
    ) : SummaryEvent

    data class TermTapped(
        val eventId: Long,
    ) : SummaryEvent

    data class EffectSelected(
        val eventId: Long,
        val effect: HitEffect,
    ) : SummaryEvent

    data object RecordToggled : SummaryEvent
}

/** what the calculator needs to know besides its own draft: the fight, the party and what the species know */
internal class CalcContext(
    val ordered: List<Combatant>,
    val mover: Combatant?,
    val ownIds: Set<Long>,
    val resistances: Map<Long, Map<DamageType, DamageState>>,
    // the participants made of a species, the calculator can write a resistance there
    val learners: Set<Long>,
)
