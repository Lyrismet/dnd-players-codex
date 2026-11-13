package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.SpellRest
import com.lyrismet.incadent.feature.me.domain.SpellPay
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_slots_level_title
import incadent.shared.generated.resources.me_spells_card_cantrip
import incadent.shared.generated.resources.me_spells_card_cast_slot
import incadent.shared.generated.resources.me_spells_card_cast_title
import incadent.shared.generated.resources.me_spells_card_chip_free
import incadent.shared.generated.resources.me_spells_card_chip_none
import incadent.shared.generated.resources.me_spells_card_chip_slot
import incadent.shared.generated.resources.me_spells_card_delete
import incadent.shared.generated.resources.me_spells_card_edit
import incadent.shared.generated.resources.me_spells_card_over
import incadent.shared.generated.resources.me_spells_meta_free_long
import incadent.shared.generated.resources.me_spells_meta_free_short
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
        draft != null -> SpellEditSheet(draft, send)
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
            if (spell.text.isNotBlank()) {
                Text(spell.text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
            }
            if (!state.readOnly) {
                if (!spell.isCantrip) CastBlock(spell, state, send)
                CardActions(spell, send)
            }
        }
    }
}

/** the ways to spend it, one chip per slot level that can hold it (or the free casts), a press casts */
@Composable
private fun CastBlock(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val columns = state.columns.filter { it.level >= spell.level }
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(
                background = AppPalette.Background,
                border = AppPalette.BorderSubtle,
            ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionOverline(stringResource(Res.string.me_spells_card_cast_title), color = AppPalette.Arcane)
            Text(
                if (spell.hasFreeCasts) {
                    freeMeta(spell)
                } else {
                    stringResource(Res.string.me_spells_card_cast_slot, spell.level.toRomanNumeral())
                },
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (spell.hasFreeCasts) {
                val left = spell.freeLeft > 0
                CastChip(
                    if (left) {
                        stringResource(
                            Res.string.me_spells_card_chip_free,
                            spell.freeLeft,
                            spell.freeMax,
                        )
                    } else {
                        stringResource(Res.string.me_spells_card_chip_none)
                    },
                    left,
                    Modifier.weight(1f),
                ) { send(SpellsEvent.Cast(spell.id, SpellPay.Free)) }
            } else {
                columns.forEach { column ->
                    val left = column.resource.current
                    CastChip(
                        stringResource(Res.string.me_spells_card_chip_slot, column.level.toRomanNumeral(), left),
                        left > 0,
                        Modifier.weight(1f),
                    ) { send(SpellsEvent.Cast(spell.id, SpellPay.Slot(column.level))) }
                }
            }
        }
    }
}

@Composable
internal fun freeMeta(spell: CharacterSpell): String =
    stringResource(
        if (spell.freeRest ==
            SpellRest.SHORT
        ) {
            Res.string.me_spells_meta_free_short
        } else {
            Res.string.me_spells_meta_free_long
        },
    )

@Composable
private fun CastChip(
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        tone = ButtonTone.Neutral,
        enabled = enabled,
        height = 48.dp,
    )
}

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
