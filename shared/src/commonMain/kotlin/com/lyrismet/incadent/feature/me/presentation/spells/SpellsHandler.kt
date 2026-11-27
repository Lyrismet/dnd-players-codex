package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.core.format.toRomanNumeral
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
import incadent.shared.generated.resources.me_spells_cast_free_sub
import incadent.shared.generated.resources.me_spells_cast_free_title
import incadent.shared.generated.resources.me_spells_cast_no_mark
import incadent.shared.generated.resources.me_spells_cast_slot_sub
import incadent.shared.generated.resources.me_spells_cast_slot_title
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
) {
    fun find(id: Long): CharacterSpell? = spells.firstOrNull { it.id == id }
}

/** what a fight did with a cast, [note] goes under the toast and [undo] takes it back with the cast */
class CastEffect(
    val note: String,
    val undo: suspend () -> Unit,
)

/** reacts to the presses of the spells block for the sheet and the fight, so a cast spends the same slots in both */
@Suppress("LongParameterList")
class SpellsHandler(
    private val ui: SpellsUiState,
    private val scope: CoroutineScope,
    private val resources: CharacterResourceRepository,
    private val spells: CharacterSpellRepository,
    private val undoController: UndoController,
    imports: SpellImportDeps,
    private val onHoldTipSeen: () -> Unit = {},
    private val inFight: Boolean = false,
    private val onCast: suspend (CharacterSpell, SpellPay) -> CastEffect? = { _, _ -> null },
) {
    private val casting = SpellCasting(resources, spells)
    private val importFlow = SpellImportFlow(ui, scope, imports)

    @Suppress("CyclomaticComplexMethod")
    fun handle(
        event: SpellsEvent,
        context: SpellsContext,
    ) {
        when (event) {
            is SpellsEvent.LevelToggled -> ui.openLevel = event.level.takeIf { it != ui.openLevel }
            SpellsEvent.CardClosed -> {
                ui.cardId = null
                ui.descriptionDraft = null
            }
            is SpellsEvent.CardOpened -> ui.cardId = event.id
            SpellsEvent.DraftClosed -> closeDraft()
            SpellsEvent.HoldTipDismissed -> onHoldTipSeen()
            SpellsEvent.UndoClicked -> undoController.undo()
            SpellsEvent.UndoDismissed -> undoController.dismiss()
            SpellsEvent.DescriptionClosed -> ui.descriptionDraft = null
            is SpellsEvent.DescriptionChanged -> ui.descriptionDraft = event.text
            is SpellsEvent.DraftChanged -> {
                ui.draft = event.spell
                importFlow.endHint()
            }
            is SpellsEvent.ImportEvent -> importFlow.handle(event, context.readOnly, nextPosition(context))
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
                context.find(event.spellId)?.let { cast(it, event.pay, context) }
            is SpellsEvent.CircleShown ->
                context.find(event.id)?.let {
                    val circle = event.circle.takeIf { c -> c > it.level } ?: 0
                    scope.launch {
                        spells.upsert(
                            context.memberId,
                            it.copy(growth = it.growth.copy(shownCircle = circle)),
                        )
                    }
                }
            is SpellsEvent.Add ->
                ui.draft =
                    CharacterSpell(
                        id = 0,
                        name = "",
                        level = event.level.coerceAtLeast(CANTRIP_LEVEL),
                        position = nextPosition(context),
                    )
            SpellsEvent.DraftSaved -> save(context)
            is SpellsEvent.Deleted ->
                context.find(event.id)?.let { delete(it, context.memberId) }
            else -> openEditor(event, context)
        }
    }

    private fun nextPosition(context: SpellsContext) = (context.spells.maxOfOrNull { it.position } ?: -1) + 1

    // what a hold or the menu of the card opens, the whole form or one block or the description field
    private fun openEditor(
        event: SpellsEvent,
        context: SpellsContext,
    ) {
        when (event) {
            is SpellsEvent.EditRequested ->
                context.find(event.id)?.let {
                    ui.cardId = null
                    ui.draft = it
                }
            is SpellsEvent.BlockHeld ->
                context.find(event.id)?.let {
                    ui.block = event.block
                    ui.draft = it
                }
            is SpellsEvent.DescriptionHeld ->
                context.find(event.id)?.let { ui.descriptionDraft = it.text }
            SpellsEvent.DescriptionSaved -> saveDescription(context)
            else -> Unit
        }
    }

    private fun cast(
        spell: CharacterSpell,
        chosen: SpellPay?,
        context: SpellsContext,
    ) {
        val pay = chosen ?: defaultPay(spell, context.columns) ?: return
        scope.launch {
            val cast = casting.cast(spell, pay, context.columns) ?: return@launch
            val effect = onCast(spell, pay)
            val (title, sub) = castTexts(spell, pay, context.columns)
            val note = effect?.note ?: getString(Res.string.me_spells_cast_no_mark).takeIf { !inFight && spell.marks }
            undoController.showSpent(title, listOfNotNull(sub, note).joinToString(" · ")) {
                effect?.undo?.invoke()
                cast.undo()
            }
        }
    }

    // a slot or a use is shown by what is left of it after the cast
    private suspend fun castTexts(
        spell: CharacterSpell,
        pay: SpellPay,
        columns: List<SlotColumn>,
    ): Pair<String, String?> =
        when (pay) {
            is SpellPay.Slot -> {
                val column = columns.firstOrNull { it.level == pay.level }
                val circle = pay.level.toRomanNumeral()
                getString(Res.string.me_spells_cast_slot_title, circle) to
                    column?.let {
                        getString(Res.string.me_spells_cast_slot_sub, circle, it.resource.current - 1, it.resource.max)
                    }
            }
            SpellPay.Free ->
                getString(Res.string.me_spells_cast_free_title, spell.name) to
                    getString(Res.string.me_spells_cast_free_sub, spell.freeLeft - 1, spell.freeMax)
            SpellPay.None -> spell.name to null
        }

    private fun closeDraft() {
        ui.draft = null
        ui.block = null
        importFlow.reset()
    }

    private fun saveDescription(context: SpellsContext) {
        val text = ui.descriptionDraft?.trim()
        ui.descriptionDraft = null
        val spell = ui.cardId?.let(context::find)
        if (text == null || spell == null || text == spell.text) return
        scope.launch { spells.upsert(context.memberId, spell.copy(text = text)) }
    }

    private fun save(context: SpellsContext) {
        val draft = ui.draft?.clamped() ?: return
        if (draft.name.isEmpty()) return
        closeDraft()
        // a new spell shows itself, in the tile of its own circle whichever way the sheet was opened
        if (draft.id == 0L) ui.openLevel = draft.level
        scope.launch {
            val stored = context.find(draft.id)
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
