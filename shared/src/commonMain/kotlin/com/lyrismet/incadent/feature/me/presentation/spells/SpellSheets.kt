package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_slots_level_title
import incadent.shared.generated.resources.me_spells_card_cantrip
import incadent.shared.generated.resources.me_spells_meta_free_long
import org.jetbrains.compose.resources.stringResource

/** the card of the open spell with the editor of a block or of the whole spell over it */
@Composable
internal fun SpellSheets(
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val card = state.card
    val draft = state.draft
    // the full form closes the card under it, a block sheet leaves it open
    if (card != null && (draft == null || state.block != null)) SpellCardSheet(card, state, send)
    if (draft != null) SpellEditSheet(draft, state, send)
    state.importSheet?.let { SpellImportSheet(it, send) }
}

@Composable
internal fun levelLabel(spell: CharacterSpell): String =
    if (spell.isCantrip) {
        stringResource(Res.string.me_spells_card_cantrip)
    } else {
        stringResource(Res.string.me_slots_level_title, spell.level.toRomanNumeral())
    }

@Composable
internal fun freeMeta(): String = stringResource(Res.string.me_spells_meta_free_long)
