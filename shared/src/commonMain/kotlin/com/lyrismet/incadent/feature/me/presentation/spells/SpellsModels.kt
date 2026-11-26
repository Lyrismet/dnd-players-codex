package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportFailure
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.SpellPay

/** what the spells block draws: the numbers, the slots, the list and the card or the editor that is open */
data class SpellsBlockState(
    val ability: Ability?,
    val saveDc: Int?,
    val attack: Int?,
    // the cantrips grow with it
    val characterLevel: Int,
    val columns: List<SlotColumn>,
    val spells: List<CharacterSpell>,
    // the level whose cells and list are open under its tile
    val openLevel: Int?,
    val card: CharacterSpell?,
    val draft: CharacterSpell?,
    val readOnly: Boolean,
    // the one-time hint of the card that its blocks can be held
    val holdTip: Boolean = false,
    // the block of the card whose own sheet is open, null while the whole form is
    val block: SpellBlock? = null,
    // the description while it is edited in place on the card
    val descriptionDraft: String? = null,
    // the toast of the last cast, drawn inside the card because a sheet covers the one of the screen
    val undo: UndoAction? = null,
    // the numbers open their sheets only on the screen that owns the sheet
    val tilesEditable: Boolean,
    // the sheet that takes the link of a spell to import
    val importSheet: SpellImportSheetState? = null,
    // the line over the form of an imported spell, null once the form is touched
    val importHint: ImportHint? = null,
)

/** the link being typed and what became of the last try */
data class SpellImportSheetState(
    val link: String = "",
    val phase: ImportPhase = ImportPhase.Idle,
)

sealed interface ImportPhase {
    data object Idle : ImportPhase

    data object Loading : ImportPhase

    data class Failed(
        val failure: SpellImportFailure,
    ) : ImportPhase
}

/** [site] is the name of the site the spell came from, [dice] the dice found in its description */
data class ImportHint(
    val site: String,
    val dice: List<String>,
)

/** a part of the card that opens its own sheet on a hold */
enum class SpellBlock { NAME, DAMAGE, GROWTH, TIME, RANGE, COMPONENTS, DURATION }

/** the three numbers on top of the block */
enum class SpellTile { ABILITY, DC, ATTACK }

sealed interface SpellsEvent {
    data class LevelToggled(
        val level: Int,
    ) : SpellsEvent

    /** a press on the cell at [index] of the slot counter [id] */
    data class CellPressed(
        val id: Long,
        val index: Int,
    ) : SpellsEvent

    /** [pay] is null for the one press of the row button, the card names the way it chose */
    data class Cast(
        val spellId: Long,
        val pay: SpellPay? = null,
    ) : SpellsEvent

    data class CardOpened(
        val id: Long,
    ) : SpellsEvent

    data object CardClosed : SpellsEvent

    data class Add(
        val level: Int,
    ) : SpellsEvent

    data class EditRequested(
        val id: Long,
    ) : SpellsEvent

    data class DraftChanged(
        val spell: CharacterSpell,
    ) : SpellsEvent

    data object DraftSaved : SpellsEvent

    data object DraftClosed : SpellsEvent

    data class BlockHeld(
        val id: Long,
        val block: SpellBlock,
    ) : SpellsEvent

    data object HoldTipDismissed : SpellsEvent

    data object UndoClicked : SpellsEvent

    data object UndoDismissed : SpellsEvent

    data class DescriptionHeld(
        val id: Long,
    ) : SpellsEvent

    data class DescriptionChanged(
        val text: String,
    ) : SpellsEvent

    data object DescriptionSaved : SpellsEvent

    data object DescriptionClosed : SpellsEvent

    data class Deleted(
        val id: Long,
    ) : SpellsEvent

    data class TileTapped(
        val tile: SpellTile,
    ) : SpellsEvent

    /** the presses of the import of a spell by its link */
    sealed interface ImportEvent : SpellsEvent

    data object ImportOpened : ImportEvent

    data class ImportLinkChanged(
        val link: String,
    ) : ImportEvent

    data object ImportSubmitted : ImportEvent

    data object ImportClosed : ImportEvent

    data object ImportHintDismissed : ImportEvent
}

/** what the screen remembers about the block: the open level, the open card and the spell being edited */
class SpellsUiState {
    var openLevel by mutableStateOf<Int?>(null)
    var cardId by mutableStateOf<Long?>(null)
    var draft by mutableStateOf<CharacterSpell?>(null)
    var block by mutableStateOf<SpellBlock?>(null)
    var descriptionDraft by mutableStateOf<String?>(null)
    var importSheet by mutableStateOf<SpellImportSheetState?>(null)
    var importHint by mutableStateOf<ImportHint?>(null)

    fun reset() {
        openLevel = null
        cardId = null
        draft = null
        block = null
        descriptionDraft = null
        importSheet = null
        importHint = null
    }
}
