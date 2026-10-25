package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormEvent
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormState
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.presentation.prep.CombatPrepEvent
import com.lyrismet.incadent.feature.combat.presentation.prep.FoeSearchState
import com.lyrismet.incadent.feature.combat.presentation.prep.HpPadState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CombatRoundScreen(
    val combatId: Long,
) : Screen

enum class PanelTab { ACTIONS, CONDITIONS, MORE }

/** who a row belongs to, the dot and the label take their colour from it */
enum class RowSide { PARTY, ALLY, ENEMY, NEUTRAL, FRIEND, FLED }

enum class EndKind { FINISH, STOP }

enum class SaveKind { SUCCESS, FAILURE }

enum class WoundTone { WHOLE, WOUNDED, BLOODIED, BARELY, DOWN, DYING, DEAD }

enum class HpLevel { HIGH, MID, LOW, ZERO }

/** the tint of a small chip under a name: a condition of its group, or what the participant became */
enum class ChipKind { CAPTURED, NEUTRAL, FRIEND, FLED, RESTRAINT, WEAKENING, EFFECT }

data class CombatRoundState(
    // the fight has not arrived yet
    val isLoading: Boolean = true,
    // the fight clock is drawn by the header from this, so the presenter does not tick every second
    val startedAt: Instant? = null,
    val round: Int = 1,
    val currentName: String = "",
    val hostile: String = "",
    val allClear: Boolean = false,
    val rows: List<RoundRow> = emptyList(),
    val panel: RoundPanel? = null,
    val tab: PanelTab = PanelTab.ACTIONS,
    val menuOpen: Boolean = false,
    val search: FoeSearchState? = null,
    val hpPad: HpPadState? = null,
    val calc: CalcState? = null,
    val summary: SummaryState? = null,
    // the damage of the current round for the header button, none while nothing was dealt in the fight
    val roundDamage: RoundDamage? = null,
    // the codex form of a reinforcement made on the spot, [formKinds] are the types it offers
    val entryForm: CodexEntryFormState? = null,
    val formKinds: List<EntityKind> = emptyList(),
    val promotion: PromotionSheet? = null,
    val toast: ToastMessage? = null,
    val eventSink: (CombatRoundEvent) -> Unit = {},
) : CircuitUiState

data class RoundDamage(
    val current: String?,
)

data class RoundRow(
    val id: Long,
    val badge: String,
    val current: Boolean,
    val selected: Boolean,
    val name: String,
    val struck: Boolean,
    val role: String,
    val side: RowSide,
    // the damage taken on the current turn
    val lastHit: String?,
    val hp: RowHp?,
    val wound: RowWound?,
    // under the right column: the estimate of what is left, or what was taken
    val caption: String,
    val chips: List<RowChip>,
    val dimmed: Boolean,
    val reaction: RowReaction?,
)

data class RowHp(
    val current: String,
    val max: String,
    val temp: Int,
    val ratio: Float,
    val level: HpLevel,
)

data class RowWound(
    val label: String,
    val tone: WoundTone,
    // how many of the five bars are filled
    val pips: Int,
    val successes: Int?,
    val failures: Int?,
)

data class RowChip(
    val label: String,
    val kind: ChipKind,
)

data class RowReaction(
    val available: Boolean,
    // a reaction that can be spent while an enemy moves
    val pulsing: Boolean,
    val label: String,
)

data class RoundPanel(
    val id: Long,
    val name: String,
    val sideLabel: String,
    val side: RowSide,
    val hp: PanelHp?,
    val wound: PanelWound?,
    val marks: List<MarkOption>?,
    val sayLine: String,
    val saves: SavesState?,
    val down: DownState?,
    val died: Boolean,
    val conditions: List<ConditionGroupItem>,
    val more: PanelMore,
    // the calculator modes this participant can take, a guessed one has no temporary hit points
    val calcModes: List<CalcMode>,
    val sourceNote: String,
)

data class PanelHp(
    val current: String,
    val max: String,
    val caption: String,
)

data class PanelWound(
    val label: String,
    val tone: WoundTone,
    val source: String,
)

data class MarkOption(
    val state: CombatState,
    val label: String,
    val tone: WoundTone,
    val selected: Boolean,
)

data class SavesState(
    val successes: Int,
    val failures: Int,
)

data class DownState(
    val canStandUp: Boolean,
    val canCapture: Boolean,
    val captured: Boolean,
    val canDie: Boolean,
)

data class ConditionGroupItem(
    val group: ConditionGroup,
    val title: String,
    val items: List<ConditionItem>,
)

data class ConditionItem(
    val condition: CombatCondition,
    val label: String,
    val on: Boolean,
)

data class PanelMore(
    val stances: List<StanceOption>?,
    val stanceHint: String,
    val fullShown: Boolean,
    val full: Boolean,
    val canSetHp: Boolean,
    val note: String,
    val notePlaceholder: String,
    // the bestiary damage of the kind this participant is made of, null when it has none to edit
    val vulnerabilities: Map<DamageType, DamageState>?,
    // under the grid: where a change goes, or why there is no grid
    val vulnerabilitiesCaption: String,
    // set for a participant that can still be given a name of the codex
    val naming: NamingState?,
)

data class StanceOption(
    val stance: CombatStance,
    val label: String,
    val selected: Boolean,
)

sealed interface CombatRoundEvent : CircuitUiEvent {
    data class Calc(
        val event: CalcEvent,
    ) : CombatRoundEvent

    data class Summary(
        val event: SummaryEvent,
    ) : CombatRoundEvent

    /** the sheet that brings reinforcements is the same one the preparation uses */
    data class Search(
        val event: CombatPrepEvent.SearchEvent,
    ) : CombatRoundEvent

    data class EntryForm(
        val event: CodexEntryFormEvent,
    ) : CombatRoundEvent

    data class RowTapped(
        val id: Long,
    ) : CombatRoundEvent

    data class TabSelected(
        val tab: PanelTab,
    ) : CombatRoundEvent

    data class ReactionToggled(
        val id: Long,
    ) : CombatRoundEvent

    data class NameChanged(
        val text: String,
    ) : CombatRoundEvent

    data class NameChosen(
        val choice: NameChoice,
    ) : CombatRoundEvent

    data class PromotionScopeChanged(
        val type: DamageType,
        val onlyNpc: Boolean,
    ) : CombatRoundEvent

    data object PromotionConfirmed : CombatRoundEvent

    data object PromotionRejected : CombatRoundEvent

    data class VulnerabilityChanged(
        val type: DamageType,
        val state: DamageState,
    ) : CombatRoundEvent

    data object NextTurn : CombatRoundEvent

    data object MenuToggled : CombatRoundEvent

    data class EndRequested(
        val kind: EndKind,
    ) : CombatRoundEvent

    data class ReinforcementOpened(
        val ally: Boolean,
    ) : CombatRoundEvent

    data class MarkSelected(
        val state: CombatState,
    ) : CombatRoundEvent

    data object StandUp : CombatRoundEvent

    data object CaptureToggled : CombatRoundEvent

    data object DiedToggled : CombatRoundEvent

    data class SaveToggled(
        val kind: SaveKind,
        val index: Int,
    ) : CombatRoundEvent

    data class ConditionToggled(
        val condition: CombatCondition,
    ) : CombatRoundEvent

    data class StanceSelected(
        val stance: CombatStance,
    ) : CombatRoundEvent

    data object FullToggled : CombatRoundEvent

    // names its participant, a save that waits for the typing to pause may arrive after another one is chosen
    data class NoteChanged(
        val memberId: Long,
        val note: String,
    ) : CombatRoundEvent

    data object RemoveClicked : CombatRoundEvent

    data object SetHpOpened : CombatRoundEvent

    data class HpDigit(
        val digit: Int,
    ) : CombatRoundEvent

    data object HpBackspace : CombatRoundEvent

    data object HpCleared : CombatRoundEvent

    data object HpApplied : CombatRoundEvent

    data object HpPadClosed : CombatRoundEvent
}
