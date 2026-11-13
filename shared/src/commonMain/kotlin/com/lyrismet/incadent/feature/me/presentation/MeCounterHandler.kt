package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.ResourceTemplate
import com.lyrismet.incadent.feature.me.domain.nextPosition
import com.lyrismet.incadent.feature.me.domain.slotMaxima

private const val NEW_RESOURCE_MAX = 1

/** reacts to the presses of the resources and the spell slots, the sheets they open go through the screen state */
internal class MeCounterHandler(
    private val ui: MeUiState,
    private val actions: MeActions,
    private val counters: MeCounterActions,
) {
    fun handle(
        event: CounterEvent,
        member: PartyMember,
        character: MeCharacter,
    ) {
        val all = character.spells.columns.map { it.resource } + character.resources
        when (event) {
            is CounterEvent.CellPressed ->
                all.firstOrNull { it.id == event.id }?.let { counters.pressCell(it.id, event.index) }
            is CounterEvent.PipHeld -> all.firstOrNull { it.id == event.id }?.let { hold(it, all) }
            CounterEvent.ResourceAdded -> ui.edit = newResource("", all)
            CounterEvent.TemplatesRequested -> ui.edit = MeOverlay.Templates
            is CounterEvent.TemplateChosen -> ui.edit = template(event, all)
            CounterEvent.ResourceDeleted -> delete(member, ui.edit, all)
            CounterEvent.SlotsRequested -> ui.edit = MeOverlay.Slots(slotMaxima(all))
            CounterEvent.SpellsHidden -> counters.setSpellsHidden(member, true)
            CounterEvent.SpellAbilityRequested,
            is CounterEvent.SpellAdjustmentRequested,
            is CounterEvent.SpellAbilityChosen,
            -> spellEvent(event, member, character)
        }
    }

    private fun spellEvent(
        event: CounterEvent,
        member: PartyMember,
        character: MeCharacter,
    ) {
        when (event) {
            CounterEvent.SpellAbilityRequested -> ui.edit = MeOverlay.SpellAbility(character.spells.ability)
            is CounterEvent.SpellAdjustmentRequested ->
                ui.edit = MeOverlay.Adjustment.of(character.sheet.source, event.target)
            is CounterEvent.SpellAbilityChosen -> {
                actions.sheet(member.id) { it.copy(spellAbility = event.ability) }
                ui.edit = null
            }
            else -> Unit
        }
    }

    private fun template(
        event: CounterEvent.TemplateChosen,
        all: List<CharacterResource>,
    ) = if (event.template == ResourceTemplate.SPELL_SLOTS) {
        MeOverlay.Slots(slotMaxima(all))
    } else {
        newResource(event.name, all)
    }

    // a slot opens the sheet of all the levels, a feature its own
    private fun hold(
        resource: CharacterResource,
        all: List<CharacterResource>,
    ) {
        ui.edit =
            if (resource.isSpellSlot) {
                MeOverlay.Slots(slotMaxima(all))
            } else {
                MeOverlay.Resource(
                    resource.id,
                    resource.name,
                    resource.max,
                    resource.current,
                    resource.position,
                    resource.originId,
                )
            }
    }

    // the stored copy is the one that comes back with the undo, not the half typed draft
    private fun delete(
        member: PartyMember,
        open: MeOverlay?,
        all: List<CharacterResource>,
    ) {
        val draft = open as? MeOverlay.Resource ?: return
        all.firstOrNull { it.id == draft.id }?.let { counters.delete(member.id, it) }
        ui.edit = null
    }

    private fun newResource(
        name: String,
        all: List<CharacterResource>,
    ) = MeOverlay.Resource(0, name, NEW_RESOURCE_MAX, NEW_RESOURCE_MAX, nextPosition(all), "")
}
