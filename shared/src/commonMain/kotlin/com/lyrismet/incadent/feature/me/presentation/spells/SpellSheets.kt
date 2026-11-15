package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_slots_level_title
import incadent.shared.generated.resources.me_spells_card_cantrip
import incadent.shared.generated.resources.me_spells_card_delete
import incadent.shared.generated.resources.me_spells_card_edit
import incadent.shared.generated.resources.me_spells_card_over
import incadent.shared.generated.resources.me_spells_meta_free_long
import org.jetbrains.compose.resources.stringResource

/** the card of the open spell and the editor of the one being made, one of them at a time */
@Composable
internal fun SpellSheets(
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val draft = state.draft
    val card = state.card
    when {
        draft != null -> SpellEditSheet(draft, state, send)
        card != null -> SpellCardSheet(card, state, send)
    }
}

@Composable
internal fun levelLabel(spell: CharacterSpell): String =
    if (spell.isCantrip) {
        stringResource(Res.string.me_spells_card_cantrip)
    } else {
        stringResource(Res.string.me_slots_level_title, spell.level.toRomanNumeral())
    }

@Composable
private fun SpellCardSheet(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val close = { send(SpellsEvent.CardClosed) }
    val over =
        if (spell.school.isBlank()) {
            levelLabel(spell)
        } else {
            stringResource(Res.string.me_spells_card_over, levelLabel(spell), spell.school)
        }
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            scroll = true,
            header = { AppSheetHeader(title = spell.name, onClose = close, overline = over.uppercase()) },
        ) {
            Badges(spell)
            CardGrid(spell)
            DamageBlock(spell, state.characterLevel)
            if (spell.text.isNotBlank()) {
                Text(spell.text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
            }
            if (!state.readOnly) {
                ApplyButton(spell, state.columns, send)
                CardActions(spell, send)
            }
        }
    }
}

@Composable
internal fun freeMeta(): String = stringResource(Res.string.me_spells_meta_free_long)

@Composable
private fun CardActions(
    spell: CharacterSpell,
    send: (SpellsEvent) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(
            text = stringResource(Res.string.me_spells_card_delete),
            onClick = { send(SpellsEvent.Deleted(spell.id)) },
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Maroon,
            variant = ButtonVariant.Ghost,
            height = 48.dp,
        )
        AppButton(
            text = stringResource(Res.string.me_spells_card_edit),
            onClick = { send(SpellsEvent.EditRequested(spell.id)) },
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Neutral,
            height = 48.dp,
        )
    }
}
