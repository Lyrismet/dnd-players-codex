package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.damage.HitScale

/** the edits of the typed sum, the types of its terms and its targets: pure, the sheet only calls them */
internal object CalcEditing {
    fun edit(
        draft: CalcDraft,
        event: CalcEvent,
    ): CalcDraft =
        when (event) {
            is CalcEvent.Digit -> typeDigit(draft, event.digit)
            CalcEvent.Plus -> draft.copy(expr = draft.expr.plus())
            CalcEvent.Backspace -> backspace(draft)
            CalcEvent.Clear -> draft.copy(expr = draft.expr.clear(), termTypes = emptyList())
            is CalcEvent.TypeSelected -> selectType(draft, event)
            is CalcEvent.TargetToggled ->
                draft.copy(selectedTarget = if (draft.selectedTarget == event.id) null else event.id)
            is CalcEvent.TargetRemoved ->
                draft.copy(
                    targets = draft.targets - event.id,
                    scales =
                        draft.scales - event.id,
                )
            is CalcEvent.TargetAdded -> draft.copy(targets = draft.targets + event.id)
            is CalcEvent.ScaleSelected ->
                draft.copy(
                    scales =
                        if (event.scale ==
                            HitScale.ONE
                        ) {
                            draft.scales - event.id
                        } else {
                            draft.scales + (event.id to event.scale)
                        },
                )
            else -> draft
        }

    /** a digit starts a term with the type chosen last */
    private fun typeDigit(
        draft: CalcDraft,
        digit: Int,
    ): CalcDraft {
        val after = draft.expr.digit(digit)
        if (after == draft.expr) return draft
        val index = after.raw.split('+').lastIndex
        val starts =
            after.raw
                .split('+')
                .last()
                .length == 1
        val types = draft.termTypes.padded(index + 1)
        return draft.copy(
            expr = after,
            termTypes =
                if (starts &&
                    types[index] == null
                ) {
                    types.toMutableList().also { it[index] = draft.currentType }
                } else {
                    types
                },
        )
    }

    private fun backspace(draft: CalcDraft): CalcDraft {
        val after = draft.expr.backspace()
        val terms = after.raw.split('+').count { it.isNotEmpty() }
        return draft.copy(expr = after, termTypes = draft.termTypes.take(terms))
    }

    /** pressing the chosen type again takes it off, a type is kept for the number being typed and for the next ones */
    private fun selectType(
        draft: CalcDraft,
        event: CalcEvent.TypeSelected,
    ): CalcDraft {
        val parts = draft.expr.raw.split('+')
        val index = parts.lastIndex
        val types = draft.termTypes.padded(index + 1).toMutableList()
        val current = if (parts[index].isNotEmpty() && types[index] != null) types[index] else draft.currentType
        val chosen = if (current == event.type) null else event.type
        if (parts[index].isNotEmpty()) types[index] = chosen
        return draft.copy(currentType = chosen, termTypes = types)
    }

    private fun <T> List<T?>.padded(size: Int): List<T?> =
        if (this.size >=
            size
        ) {
            this
        } else {
            this + List(size - this.size) { null }
        }
}
