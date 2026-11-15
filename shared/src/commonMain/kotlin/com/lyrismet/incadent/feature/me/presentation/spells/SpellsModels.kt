package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
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
    // the numbers open their sheets only on the screen that owns the sheet
    val tilesEditable: Boolean,
)

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

    data class Deleted(
        val id: Long,
    ) : SpellsEvent

    data class TileTapped(
        val tile: SpellTile,
    ) : SpellsEvent
}

/** what the screen remembers about the block: the open level, the open card and the spell being edited */
class SpellsUiState {
    var openLevel by mutableStateOf<Int?>(null)
    var cardId by mutableStateOf<Long?>(null)
    var draft by mutableStateOf<CharacterSpell?>(null)

    fun reset() {
        openLevel = null
        cardId = null
        draft = null
    }
}
