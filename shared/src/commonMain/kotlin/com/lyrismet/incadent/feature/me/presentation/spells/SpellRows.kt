package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.me.domain.SpellPay
import com.lyrismet.incadent.feature.me.domain.defaultPay
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_cast
import incadent.shared.generated.resources.me_spells_cast_free_left
import incadent.shared.generated.resources.me_spells_cast_level
import incadent.shared.generated.resources.me_spells_cast_no_free
import incadent.shared.generated.resources.me_spells_cast_no_slot
import incadent.shared.generated.resources.me_spells_meta_cantrip
import incadent.shared.generated.resources.me_spells_meta_cantrip_school
import incadent.shared.generated.resources.me_spells_meta_slot
import incadent.shared.generated.resources.me_spells_meta_slot_time
import org.jetbrains.compose.resources.stringResource

/** the spells of one level in one dark box, a line between them */
@Composable
internal fun SpellList(
    list: List<CharacterSpell>,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.BorderSubtle)) {
        list.forEachIndexed { index, spell ->
            if (index > 0) AppDivider()
            SpellRow(spell, state, send)
        }
    }
}

/** the name with its cost under it and the button that spends it, a press on the row opens the card */
@Composable
private fun SpellRow(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val pay = defaultPay(spell, state.columns)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(background = AppPalette.Background, onClick = { send(SpellsEvent.CardOpened(spell.id)) })
                .padding(start = 12.dp, end = 6.dp)
                .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    spell.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (spell.isCantrip || pay != null) AppPalette.TextPrimary else AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spell.concentration) Text("◎", style = AppText.caption, color = AppPalette.GoldBright)
            }
            Text(
                metaLine(spell),
                style = AppText.caption,
                color = AppPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!state.readOnly && !spell.isCantrip) {
            AppButton(
                text = castLabel(spell, pay),
                onClick = { send(SpellsEvent.Cast(spell.id)) },
                enabled = pay != null,
                variant = ButtonVariant.Tinted,
                height = 40.dp,
            )
        }
    }
}

@Composable
private fun metaLine(spell: CharacterSpell): String =
    when {
        spell.isCantrip && spell.school.isNotBlank() ->
            stringResource(Res.string.me_spells_meta_cantrip_school, spell.school.lowercase())
        spell.isCantrip -> stringResource(Res.string.me_spells_meta_cantrip)
        spell.hasFreeCasts ->
            freeMeta()
        spell.castingTime.isNotBlank() -> stringResource(Res.string.me_spells_meta_slot_time, spell.castingTime)
        else -> stringResource(Res.string.me_spells_meta_slot)
    }

@Composable
private fun castLabel(
    spell: CharacterSpell,
    pay: SpellPay?,
): String =
    when {
        spell.hasFreeCasts && pay == null -> stringResource(Res.string.me_spells_cast_no_free)
        spell.hasFreeCasts -> stringResource(Res.string.me_spells_cast_free_left, spell.freeLeft, spell.freeMax)
        pay is SpellPay.Slot -> stringResource(Res.string.me_spells_cast_level, pay.level.toRomanNumeral())
        pay == null -> stringResource(Res.string.me_spells_cast_no_slot)
        else -> stringResource(Res.string.me_spells_cast)
    }
