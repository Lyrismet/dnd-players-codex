package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.combat.domain.model.CustomCondition
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository

/** the marks of the chosen participant: the sheet that makes one, taking one off, the states of the campaign */
internal class MarkHandler(
    private val editor: RoundEditor,
    private val selected: MutableState<Long?>,
    private val draft: MutableState<MarkDraft?>,
    private val repository: CombatRepository,
) {
    fun onEvent(
        event: CombatRoundEvent.MarkEvent,
        customs: List<CustomCondition>,
    ) {
        val current = draft.value
        when (event) {
            is CombatRoundEvent.MarkSheetOpened ->
                selected.value?.let {
                    draft.value = MarkDraft(it, custom = event.custom, concentration = event.concentration)
                }
            is CombatRoundEvent.MarkTextChanged -> draft.value = current?.copy(text = event.text)
            CombatRoundEvent.MarkConcentrationToggled ->
                draft.value = current?.copy(concentration = !current.concentration)
            is CombatRoundEvent.MarkRoundsChanged ->
                draft.value = current?.copy(roundsText = event.text.filter { it.isDigit() }.take(MARK_ROUNDS_DIGITS))
            CombatRoundEvent.MarkSheetClosed -> draft.value = null
            CombatRoundEvent.MarkConfirmed -> current?.let { confirm(it, customs) }
            is CombatRoundEvent.MarkRemoved -> editor.edit { repository.removeMark(event.id) }
            is CombatRoundEvent.CustomToggled -> toggleCustom(event.name)
        }
    }

    private fun confirm(
        d: MarkDraft,
        customs: List<CustomCondition>,
    ) {
        val text = d.text.trim()
        val concentration = d.concentration && !d.custom
        if (text.isEmpty() && !concentration) return
        draft.value = null
        val rounds = d.roundsText.toIntOrNull()?.takeIf { it > 0 }
        editor.withMember(d.memberId) { combat, member ->
            when {
                d.custom -> {
                    if (customs.none { it.name == text }) repository.addCustomCondition(text)
                    repository.addMark(member.id, MarkKind.CUSTOM, text, rounds, combat.round)
                }
                concentration -> repository.addMark(member.id, MarkKind.CONCENTRATION, text, rounds, combat.round)
                else -> repository.addMark(member.id, MarkKind.FACT, text, rounds, combat.round)
            }
        }
    }

    private fun toggleCustom(name: String) {
        val id = selected.value ?: return
        editor.withMember(id) { combat, member ->
            val on = member.marks.firstOrNull { it.kind == MarkKind.CUSTOM && it.text == name }
            if (on != null) {
                repository.removeMark(on.id)
            } else {
                repository.addMark(member.id, MarkKind.CUSTOM, name, null, combat.round)
            }
        }
    }
}
