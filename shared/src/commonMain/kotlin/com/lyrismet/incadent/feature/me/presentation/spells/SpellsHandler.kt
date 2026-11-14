package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_LEVEL
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.SpellCasting
import com.lyrismet.incadent.feature.me.domain.SpellPay
import com.lyrismet.incadent.feature.me.domain.defaultPay
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_deleted_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** what a screen hands the handler on every press, read fresh so a slow press never works on an old list */
class SpellsContext(
    val memberId: Long,
    val spells: List<CharacterSpell>,
    val columns: List<SlotColumn>,
    val readOnly: Boolean,
)

/**
 * reacts to the presses of the spells block, the sheet of the character and the fight both delegate here so a cast
 * spends the same slots wherever it happens, [onCast] lets the fight put a mark for what lasts
 */
class SpellsHandler(
    private val ui: SpellsUiState,
    private val scope: CoroutineScope,
    private val resources: CharacterResourceRepository,
    private val spells: CharacterSpellRepository,
    private val undoController: UndoController,
    private val onCast: suspend (CharacterSpell, SpellPay) -> Unit = { _, _ -> },
) {
    private val casting = SpellCasting(resources, spells)

    fun handle(
        event: SpellsEvent,
        context: SpellsContext,
    ) {
        when (event) {
            is SpellsEvent.LevelToggled -> ui.openLevel = event.level.takeIf { it != ui.openLevel }
            SpellsEvent.CardClosed -> ui.cardId = null
            is SpellsEvent.CardOpened -> ui.cardId = event.id
            SpellsEvent.DraftClosed -> ui.draft = null
            is SpellsEvent.DraftChanged -> ui.draft = event.spell
            is SpellsEvent.TileTapped -> Unit
            else -> if (!context.readOnly) write(event, context)
        }
    }

    private fun write(
        event: SpellsEvent,
        context: SpellsContext,
    ) {
        when (event) {
            is SpellsEvent.CellPressed -> scope.launch { resources.pressCell(event.id, event.index) }
            is SpellsEvent.Cast ->
                context.spells.firstOrNull { it.id == event.spellId }?.let { cast(it, event.pay, context) }
            is SpellsEvent.Add ->
                ui.draft =
                    CharacterSpell(
                        id = 0,
                        name = "",
                        level = event.level.coerceAtLeast(CANTRIP_LEVEL),
                        position = (context.spells.maxOfOrNull { it.position } ?: -1) + 1,
                    )
            is SpellsEvent.EditRequested ->
                context.spells.firstOrNull { it.id == event.id }?.let {
                    ui.cardId = null
                    ui.draft = it
                }
            SpellsEvent.DraftSaved -> save(context)
            is SpellsEvent.Deleted ->
                context.spells.firstOrNull { it.id == event.id }?.let { delete(it, context.memberId) }
            else -> Unit
        }
    }

    private fun cast(
        spell: CharacterSpell,
        chosen: SpellPay?,
        context: SpellsContext,
    ) {
        val pay = chosen ?: defaultPay(spell, context.columns) ?: return
        ui.cardId = null
        scope.launch {
            casting.cast(spell, pay, context.columns) ?: return@launch
            onCast(spell, pay)
        }
    }

    private fun save(context: SpellsContext) {
        val draft = ui.draft?.clamped() ?: return
        if (draft.name.isEmpty()) return
        ui.draft = null
        scope.launch {
            val stored = context.spells.firstOrNull { it.id == draft.id }
            // a spell that had no free casts starts with all of them
            val fresh = (stored?.freeMax ?: 0) == 0
            spells.upsert(context.memberId, if (fresh) draft.copy(freeLeft = draft.freeMax) else draft)
        }
    }

    private fun delete(
        spell: CharacterSpell,
        memberId: Long,
    ) {
        ui.cardId = null
        scope.launch {
            spells.delete(spell.id)
            undoController.show(getString(Res.string.me_spells_deleted_title), spell.name) {
                spells.upsert(memberId, spell.copy(id = 0))
            }
        }
    }
}
