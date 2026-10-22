package com.lyrismet.incadent.feature.combat.presentation.prep

import com.lyrismet.incadent.core.toast.ToastMessage
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class CombatPrepScreen(
    val combatId: Long,
) : Screen

/** the two sides a record can be put on, the party is its own list */
enum class PrepSide { ALLY, ENEMY }

enum class QuickKind { PARTY, ENEMY }

enum class HpMode { HP, TEMP }

data class CombatPrepState(
    // "Итоги попадут в" and the session it names, the session is the accented part
    val targetPrefix: String = "",
    val target: String = "",
    // shown instead of the lists while the codex has no party member or no enemy to fight with
    val intro: PrepIntro? = null,
    val partyIncluded: Int = 0,
    val party: List<PrepPartyRow> = emptyList(),
    val allies: List<PrepMemberRow> = emptyList(),
    val enemies: List<PrepMemberRow> = emptyList(),
    // the order of turns, shown once there is an enemy to fight
    val order: List<PrepOrderRow>? = null,
    // what the button under the lists says, none while the intro is shown
    val start: PrepStart? = null,
    val search: FoeSearchState? = null,
    val quickCreate: QuickCreateState? = null,
    val hpPad: HpPadState? = null,
    val exitConfirmVisible: Boolean = false,
    val toast: ToastMessage? = null,
    val eventSink: (CombatPrepEvent) -> Unit = {},
) : CircuitUiState

data class PrepIntro(
    val partyDone: Boolean,
    val partySub: String,
    val foeDone: Boolean,
    val foeSub: String,
)

data class PrepPartyRow(
    val partyMemberId: Long,
    val name: String,
    val sub: String,
    val included: Boolean,
    // only the player's own character is set by numbers
    val hp: PrepHp?,
)

data class PrepHp(
    val value: String,
    val caption: String,
)

enum class OrderSide { PARTY, ALLY, ENEMY }

/** one participant in the order, [number] is the place the master gave it */
data class PrepOrderRow(
    val memberId: Long,
    val name: String,
    val sub: String,
    val side: OrderSide,
    val number: Int?,
)

data class PrepStart(
    val label: String,
    val ready: Boolean,
)

/** one kind of member on a side, [count] of them, [key] names the source of the group */
data class PrepMemberRow(
    val key: String,
    val name: String,
    val sub: String,
    val count: Int,
    val canIncrease: Boolean,
)

/** the open "find a record or type a name" sheet of one side */
data class FoeSearchState(
    val side: PrepSide,
    val query: String,
    val rows: List<FoeRow>,
    val canCreate: Boolean,
    val done: Int,
    // a record put on a side in the middle of the fight, a named npc cannot be taken out again
    val reinforcement: Boolean = false,
)

data class FoeRow(
    val key: String,
    val name: String,
    val sub: String,
    val isSpecies: Boolean,
    val count: Int,
)

data class QuickCreateState(
    val kind: QuickKind,
    val name: String,
)

/** the keypad of the hit points set before the fight, [expr] and [result] are what the keys typed */
data class HpPadState(
    val name: String,
    val mode: HpMode,
    val result: String,
    val preview: String,
    val canApply: Boolean,
    // the two-way switch between hit points and temporary hit points, one-way pads leave it out
    val modesVisible: Boolean = true,
)

sealed interface CombatPrepEvent : CircuitUiEvent {
    /** a change of who is on which side */
    sealed interface MemberEvent : CombatPrepEvent

    sealed interface SearchEvent : CombatPrepEvent

    sealed interface TypedNameEvent : SearchEvent

    sealed interface QuickEvent : CombatPrepEvent

    sealed interface HpEvent : CombatPrepEvent

    sealed interface ExitEvent : CombatPrepEvent

    sealed interface StartEvent : CombatPrepEvent

    data class PartyToggled(
        val partyMemberId: Long,
    ) : MemberEvent

    data class MemberIncreased(
        val side: PrepSide,
        val key: String,
    ) : MemberEvent

    data class MemberDecreased(
        val side: PrepSide,
        val key: String,
    ) : MemberEvent

    data class OrderTapped(
        val memberId: Long,
    ) : MemberEvent

    data object OrderReset : MemberEvent

    data object StartClicked : StartEvent

    data class SearchOpened(
        val side: PrepSide,
    ) : SearchEvent

    data class QueryChanged(
        val query: String,
    ) : SearchEvent

    /** a record row was pressed - it is added, or for a named npc taken out again */
    data class FoeTapped(
        val key: String,
    ) : SearchEvent

    data class FoeDecreased(
        val key: String,
    ) : SearchEvent

    /** the typed name goes in as a member of this fight only */
    data object TempAdded : TypedNameEvent

    data object NpcCreated : TypedNameEvent

    data object SpeciesCreated : TypedNameEvent

    data object SearchClosed : SearchEvent

    data class QuickOpened(
        val kind: QuickKind,
    ) : QuickEvent

    data class QuickNameChanged(
        val name: String,
    ) : QuickEvent

    data object QuickConfirmed : QuickEvent

    data object QuickClosed : QuickEvent

    data class HpPadOpened(
        val partyMemberId: Long,
    ) : HpEvent

    data class HpModeChanged(
        val mode: HpMode,
    ) : HpEvent

    data class HpDigit(
        val digit: Int,
    ) : HpEvent

    data object HpBackspace : HpEvent

    data object HpCleared : HpEvent

    data object HpApplied : HpEvent

    data object HpPadClosed : HpEvent

    data object CloseClicked : ExitEvent

    data object ExitConfirmed : ExitEvent

    data object ExitDismissed : ExitEvent
}
