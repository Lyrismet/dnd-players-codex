package com.lyrismet.incadent.feature.combat.presentation.results

import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.results.CodexEntry
import com.lyrismet.incadent.feature.combat.presentation.summary.CombatSummary
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** the results of a fight: before they are saved to pick the outcome and the ticks, after only to read */
@Serializable
data class CombatResultsScreen(
    val combatId: Long,
    // opened from "Прервать бой", the outcome starts at "Прервано"
    val interrupted: Boolean = false,
    val readOnly: Boolean = false,
) : Screen

data class CombatResultsState(
    val loaded: Boolean = false,
    val readOnly: Boolean = false,
    val title: String = "",
    val meta: String = "",
    // "Сохранится в:" and the session it names
    val targetPrefix: String = "",
    val target: String = "",
    val suggested: CombatOutcome = CombatOutcome.VICTORY,
    val outcome: CombatOutcome = CombatOutcome.VICTORY,
    val summary: CombatSummary? = null,
    val logExpanded: Boolean = false,
    val checklist: List<CodexEntry> = emptyList(),
    val deleteConfirmVisible: Boolean = false,
    val saving: Boolean = false,
    val toast: ToastMessage? = null,
    val eventSink: (CombatResultsEvent) -> Unit = {},
) : CircuitUiState

sealed interface CombatResultsEvent : CircuitUiEvent {
    data class OutcomeSelected(
        val outcome: CombatOutcome,
    ) : CombatResultsEvent

    data object LogToggled : CombatResultsEvent

    data class ItemToggled(
        val key: String,
    ) : CombatResultsEvent

    data object SaveClicked : CombatResultsEvent

    /** back to the fight, or away from the read-only results */
    data object BackClicked : CombatResultsEvent

    data object OpenSessionClicked : CombatResultsEvent

    data object DeleteClicked : CombatResultsEvent

    data object DeleteConfirmed : CombatResultsEvent

    data object DeleteDismissed : CombatResultsEvent
}
