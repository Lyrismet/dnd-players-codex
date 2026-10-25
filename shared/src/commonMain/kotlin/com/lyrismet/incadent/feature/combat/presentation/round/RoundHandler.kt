package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.calc.CalcExpression
import com.lyrismet.incadent.feature.combat.domain.round.toggledSaves
import com.lyrismet.incadent.feature.combat.presentation.prep.CombatPrepEvent
import com.lyrismet.incadent.feature.combat.presentation.prep.FoeSearchController
import com.lyrismet.incadent.feature.combat.presentation.prep.PrepSide
import com.lyrismet.incadent.feature.combat.presentation.prep.SearchDraft
import com.lyrismet.incadent.feature.combat.presentation.results.CombatResultsScreen
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.CoroutineScope

/** what the keypad of the hit points has typed for one participant */
internal data class RoundHpDraft(
    val memberId: Long,
    val expr: CalcExpression = CalcExpression(),
)

/** what the screen was asked to do at the moment of an event */
internal class RoundSnapshot(
    val selectedId: Long?,
    val ownIds: Set<Long>,
)

@Suppress("LongParameterList")
internal class RoundHandler(
    private val editor: RoundEditor,
    private val navigator: Navigator,
    private val selected: MutableState<Long?>,
    private val tab: MutableState<PanelTab>,
    private val menuOpen: MutableState<Boolean>,
    private val hpPad: MutableState<RoundHpDraft?>,
    search: MutableState<SearchDraft?>,
    private val form: RoundForm,
    private val scope: CoroutineScope,
) {
    private val searchController = FoeSearchController(search, editor, form.host::open)

    fun onEvent(
        event: CombatRoundEvent,
        snapshot: RoundSnapshot,
        nameOf: (String) -> String?,
    ) {
        when (event) {
            is CombatRoundEvent.Search -> searchController.onEvent(event.event, nameOf)
            is CombatRoundEvent.EntryForm -> form.host.onEvent(event.event, scope)
            is CombatRoundEvent.RowTapped -> selected.value = if (selected.value == event.id) null else event.id
            is CombatRoundEvent.TabSelected -> tab.value = event.tab
            CombatRoundEvent.MenuToggled -> menuOpen.value = !menuOpen.value
            CombatRoundEvent.NextTurn ->
                editor.next(snapshot.ownIds) { id ->
                    selected.value = id
                    tab.value = PanelTab.ACTIONS
                    menuOpen.value = false
                }
            is CombatRoundEvent.EndRequested -> requestEnd(event.kind)
            is CombatRoundEvent.ReinforcementOpened ->
                searchController.onEvent(
                    CombatPrepEvent.SearchOpened(if (event.ally) PrepSide.ALLY else PrepSide.ENEMY),
                    nameOf,
                )
            is CombatRoundEvent.ReactionToggled ->
                editor.withMember(
                    event.id,
                ) { _, member -> editor.actions.toggleReaction(member) }
            else -> onMember(event, snapshot)
        }
    }

    private fun requestEnd(kind: EndKind) {
        menuOpen.value = false
        navigator.goTo(CombatResultsScreen(editor.combatId, interrupted = kind == EndKind.STOP))
    }

    /** the changes to the chosen participant */
    private fun onMember(
        event: CombatRoundEvent,
        snapshot: RoundSnapshot,
    ) {
        val id = snapshot.selectedId ?: return
        when (event) {
            is CombatRoundEvent.MarkSelected ->
                editor.withMember(
                    id,
                ) { c, m -> editor.actions.setMark(c, m, event.state) }
            CombatRoundEvent.StandUp -> editor.withMember(id) { c, m -> editor.actions.standUp(c, m) }
            CombatRoundEvent.CaptureToggled -> editor.withMember(id) { c, m -> editor.actions.toggleCaptured(c, m) }
            CombatRoundEvent.DiedToggled -> editor.withMember(id) { c, m -> editor.actions.toggleDead(c, m) }
            is CombatRoundEvent.SaveToggled ->
                editor.withMember(id) { _, m ->
                    val saves = if (event.kind == SaveKind.SUCCESS) m.deathSuccesses else m.deathFailures
                    val next = toggledSaves(saves, event.index)
                    if (event.kind == SaveKind.SUCCESS) {
                        editor.actions.setDeathSaves(m, next, m.deathFailures)
                    } else {
                        editor.actions.setDeathSaves(m, m.deathSuccesses, next)
                    }
                }
            else -> onDetails(event, id)
        }
    }

    private fun onDetails(
        event: CombatRoundEvent,
        id: Long,
    ) {
        when (event) {
            is CombatRoundEvent.ConditionToggled ->
                editor.withMember(
                    id,
                ) { c, m -> editor.actions.toggleCondition(c, m, event.condition) }
            is CombatRoundEvent.StanceSelected -> editor.setStance(id, event.stance)
            CombatRoundEvent.FullToggled ->
                editor.withMember(
                    id,
                ) { _, m -> editor.actions.setEnteredWhole(m, !m.enteredWhole) }
            is CombatRoundEvent.NoteChanged ->
                editor.withMember(event.memberId) { _, m -> editor.actions.setNote(m, event.note) }
            CombatRoundEvent.RemoveClicked -> editor.remove(id) { selected.value = null }
            else -> onHp(event, id)
        }
    }

    private fun onHp(
        event: CombatRoundEvent,
        id: Long,
    ) {
        val draft = hpPad.value
        when (event) {
            CombatRoundEvent.SetHpOpened -> hpPad.value = RoundHpDraft(id)
            is CombatRoundEvent.HpDigit -> hpPad.value = draft?.copy(expr = draft.expr.digit(event.digit))
            CombatRoundEvent.HpBackspace -> hpPad.value = draft?.copy(expr = draft.expr.backspace())
            CombatRoundEvent.HpCleared -> hpPad.value = draft?.copy(expr = CalcExpression())
            CombatRoundEvent.HpPadClosed -> hpPad.value = null
            CombatRoundEvent.HpApplied ->
                draft?.takeIf { it.expr.raw.isNotEmpty() }?.let { d ->
                    hpPad.value = null
                    editor.withMember(d.memberId) { _, m -> editor.actions.setHp(m, d.expr.value()) }
                }
            else -> Unit
        }
    }
}
