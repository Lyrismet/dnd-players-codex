package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.isValidDice
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_block_growth
import incadent.shared.generated.resources.me_spells_block_name
import incadent.shared.generated.resources.me_spells_block_save
import incadent.shared.generated.resources.me_spells_card_components
import incadent.shared.generated.resources.me_spells_card_damage
import incadent.shared.generated.resources.me_spells_card_duration
import incadent.shared.generated.resources.me_spells_card_edit
import incadent.shared.generated.resources.me_spells_card_range
import incadent.shared.generated.resources.me_spells_description_cancel
import incadent.shared.generated.resources.me_spells_edit_add
import incadent.shared.generated.resources.me_spells_edit_cast_time
import incadent.shared.generated.resources.me_spells_edit_name
import incadent.shared.generated.resources.me_spells_edit_name_hint
import incadent.shared.generated.resources.me_spells_edit_new
import incadent.shared.generated.resources.me_spells_edit_overline
import incadent.shared.generated.resources.me_spells_edit_text
import incadent.shared.generated.resources.me_spells_edit_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** the editor of one spell: the fields in the order of the mockup, a copy of the spell goes up on every change */
@Composable
internal fun SpellEditSheet(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val close = { send(SpellsEvent.DraftClosed) }
    val change = { next: CharacterSpell -> send(SpellsEvent.DraftChanged(next)) }
    val isNew = spell.id == 0L
    val block = state.block
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(editTitle(block, isNew)),
                    onClose = close,
                    overline =
                        if (block ==
                            null
                        ) {
                            stringResource(Res.string.me_spells_edit_overline)
                        } else {
                            spell.name.uppercase()
                        },
                )
            },
        ) {
            if (block == null) {
                WholeForm(spell, state, change)
            } else {
                BlockForm(block, spell, state, change)
            }
            val enabled = spell.name.isNotBlank() && spell.hasValidDice()
            if (block == null) {
                FormPrimaryButton(
                    text = stringResource(if (isNew) Res.string.me_spells_edit_add else Res.string.me_spells_card_edit),
                    onClick = { send(SpellsEvent.DraftSaved) },
                    enabled = enabled,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = stringResource(Res.string.me_spells_description_cancel),
                        onClick = close,
                        modifier = Modifier.weight(1f),
                        tone = ButtonTone.Neutral,
                        height = 52.dp,
                    )
                    AppButton(
                        text = stringResource(Res.string.me_spells_block_save),
                        onClick = { send(SpellsEvent.DraftSaved) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Solid,
                        enabled = enabled,
                        height = 52.dp,
                    )
                }
            }
        }
    }
}

private fun editTitle(
    block: SpellBlock?,
    isNew: Boolean,
): StringResource =
    when (block) {
        null -> if (isNew) Res.string.me_spells_edit_new else Res.string.me_spells_edit_title
        SpellBlock.NAME -> Res.string.me_spells_block_name
        SpellBlock.DAMAGE -> Res.string.me_spells_card_damage
        SpellBlock.GROWTH -> Res.string.me_spells_block_growth
        SpellBlock.TIME -> Res.string.me_spells_edit_cast_time
        SpellBlock.RANGE -> Res.string.me_spells_card_range
        SpellBlock.COMPONENTS -> Res.string.me_spells_card_components
        SpellBlock.DURATION -> Res.string.me_spells_card_duration
    }

@Composable
private fun WholeForm(
    spell: CharacterSpell,
    state: SpellsBlockState,
    change: (CharacterSpell) -> Unit,
) {
    NameFields(spell, change)
    CastTimeFields(spell, change)
    RangeFields(spell, change)
    ComponentFields(spell, change)
    DamageFields(spell, change)
    GrowthFields(spell, state.characterLevel, state.maxCircle(spell), change)
    DurationFields(spell, change)
    FlagCards(spell, change)
    FormTextArea(stringResource(Res.string.me_spells_edit_text), spell.text, { change(spell.copy(text = it)) })
}

/** only the fields of the block that was held, in the order the whole form has them */
@Composable
private fun BlockForm(
    block: SpellBlock,
    spell: CharacterSpell,
    state: SpellsBlockState,
    change: (CharacterSpell) -> Unit,
) {
    when (block) {
        SpellBlock.NAME -> NameFields(spell, change)
        SpellBlock.DAMAGE -> DamageFields(spell, change)
        SpellBlock.GROWTH -> GrowthFields(spell, state.characterLevel, state.maxCircle(spell), change)
        SpellBlock.TIME -> CastTimeFields(spell, change)
        SpellBlock.RANGE -> RangeFields(spell, change)
        SpellBlock.COMPONENTS -> ComponentFields(spell, change)
        SpellBlock.DURATION -> {
            DurationFields(spell, change)
            FlagCards(spell, change)
        }
    }
}

@Composable
private fun NameFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.me_spells_edit_name),
        value = spell.name,
        onChange = { change(spell.copy(name = it)) },
        placeholder = stringResource(Res.string.me_spells_edit_name_hint),
        required = true,
    )
    LevelTiles(spell.level) { change(spell.copy(level = it)) }
    SchoolField(spell.name, spell.school) { change(spell.copy(school = it)) }
}

private fun SpellsBlockState.maxCircle(spell: CharacterSpell) = columns.maxOfOrNull { it.level } ?: spell.level

private fun CharacterSpell.hasValidDice() =
    damage.all { isValidDice(it.dice) } &&
        growth.rules.all { it.target != GrowthTarget.DICE || isValidDice(it.add) } &&
        growth.rules.all { rule -> rule.steps.all { rule.target != GrowthTarget.DICE || isValidDice(it.value) } }
