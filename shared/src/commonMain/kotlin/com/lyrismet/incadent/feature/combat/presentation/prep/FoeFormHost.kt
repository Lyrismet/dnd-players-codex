package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlinx.coroutines.CoroutineScope

/** the codex form of a fighter made on the spot: what it saves joins the side it was opened for */
internal class FoeFormHost(
    private val controller: CodexEntryFormController,
    private val target: MutableState<CombatSide?>,
    private val adder: FoeAdder,
    private val imageCompressor: ImageCompressor,
) {
    /** a character for the party, an npc for either side that can be switched to a species */
    fun open(
        side: CombatSide,
        name: String = "",
        asSpecies: Boolean = false,
    ) {
        target.value = side
        if (asSpecies && side != CombatSide.PARTY) {
            controller.onAddEntryClicked(EntityKind.SPECIES, name)
            return
        }
        when (side) {
            CombatSide.PARTY -> controller.onAddEntryClicked(EntityKind.PARTY, name)
            CombatSide.ENEMY -> controller.onAddEntryClicked(EntityKind.NPC, name, npcStatus = NpcStatus.ENEMY)
            CombatSide.ALLY -> controller.onAddEntryClicked(EntityKind.NPC, name, npcStatus = NpcStatus.FRIEND)
        }
    }

    /** the types the open form offers, a party member is only a party member */
    fun kinds(): List<EntityKind> =
        if (target.value == CombatSide.PARTY) listOf(EntityKind.PARTY) else listOf(EntityKind.NPC, EntityKind.SPECIES)

    fun onEvent(
        event: CodexEntryFormEvent,
        scope: CoroutineScope,
    ) {
        controller.onEvent(
            event,
            scope,
            imageCompressor,
            onSaved = { kind, id -> target.value?.let { adder.addSaved(it, kind, id) } },
            onQuickAdded = {},
        )
    }
}
