package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource

/** what putting records on a side does: sets the sides up in the preparation, brings reinforcements in a fight */
internal interface FoeAdder {
    fun add(
        side: CombatSide,
        source: MemberSource,
    )

    fun removeOne(
        side: CombatSide,
        source: MemberSource,
    )

    fun toggle(
        side: CombatSide,
        source: MemberSource,
    )

    /** a record made in the codex form for [side], it joins the fight at once */
    fun addSaved(
        side: CombatSide,
        kind: EntityKind,
        id: Long,
    )
}

/** the events of the "find a record or type a name" sheet, the same in the preparation and in the fight */
internal class FoeSearchController(
    private val search: MutableState<SearchDraft?>,
    private val adder: FoeAdder,
    // opens the codex form for a side, with the typed name
    private val onCreate: (CombatSide, String, Boolean) -> Unit,
) {
    fun onEvent(
        event: CombatPrepEvent.SearchEvent,
        nameOf: (String) -> String?,
    ) {
        val draft = search.value
        when (event) {
            is CombatPrepEvent.SearchOpened -> search.value = SearchDraft(event.side)
            is CombatPrepEvent.QueryChanged -> search.value = draft?.copy(query = event.query)
            CombatPrepEvent.SearchClosed -> search.value = null
            is CombatPrepEvent.FoeTapped ->
                draft?.let { d -> sourceOf(event.key, nameOf)?.let { adder.toggle(d.side.toCombatSide(), it) } }
            is CombatPrepEvent.FoeDecreased ->
                draft?.let { d -> sourceOf(event.key, nameOf)?.let { adder.removeOne(d.side.toCombatSide(), it) } }
            is CombatPrepEvent.TypedNameEvent ->
                draft?.typedName()?.let { (side, name) ->
                    onTypedName(event, side, name)
                }
        }
    }

    private fun onTypedName(
        event: CombatPrepEvent.TypedNameEvent,
        side: CombatSide,
        name: String,
    ) {
        when (event) {
            CombatPrepEvent.TempAdded -> adder.add(side, MemberSource(null, null, name))
            CombatPrepEvent.CreateRequested -> onCreate(side, name, false)
            CombatPrepEvent.CreateSpeciesRequested -> onCreate(side, name, true)
        }
    }

    /** the typed name and its side, the field is emptied since the name is taken */
    private fun SearchDraft.typedName(): Pair<CombatSide, String>? =
        query.trim().takeIf { it.isNotEmpty() }?.also { search.value = copy(query = "") }?.let {
            side.toCombatSide() to
                it
        }
}
