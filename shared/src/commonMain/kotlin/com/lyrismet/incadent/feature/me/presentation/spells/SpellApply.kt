package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.atCircle
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.lyrismet.incadent.feature.me.domain.SpellPay
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_apply
import incadent.shared.generated.resources.me_spells_apply_button
import incadent.shared.generated.resources.me_spells_apply_free
import incadent.shared.generated.resources.me_spells_apply_none
import incadent.shared.generated.resources.me_spells_apply_title
import org.jetbrains.compose.resources.stringResource

private val CircleWidth = 28.dp

/** the button of the card, a press asks which slot, a cantrip has none to ask for */
@Composable
internal fun ApplyButton(
    spell: CharacterSpell,
    columns: List<SlotColumn>,
    send: (SpellsEvent) -> Unit,
) {
    val options = remember(columns, spell.level) { columns.filter { it.level >= spell.level }.sortedBy { it.level } }
    if (spell.isCantrip || options.isEmpty()) return
    var open by remember(spell.id) { mutableStateOf(false) }
    AppButton(
        text = stringResource(Res.string.me_spells_apply),
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Solid,
        height = 48.dp,
    )
    if (open) {
        AppBottomSheet(onDismissRequest = { open = false }) {
            ApplySheetBody(spell, options, send) { open = false }
        }
    }
}

/** the slots a spell can be cast with, each with what the spell comes to on it, and the button that casts */
@Composable
private fun ApplySheetBody(
    spell: CharacterSpell,
    options: List<SlotColumn>,
    send: (SpellsEvent) -> Unit,
    onClose: () -> Unit,
) {
    // the lowest circle that still has a slot, else the lowest there is
    var chosen by remember(spell.id) {
        mutableStateOf(
            (
                options.firstOrNull { it.resource.current > 0 }
                    ?: options.first()
            ).level,
        )
    }
    val current = options.firstOrNull { it.level == chosen } ?: options.first()
    AppSheetColumn(
        scroll = true,
        header = { AppSheetHeader(title = stringResource(Res.string.me_spells_apply_title), onClose = onClose) },
    ) {
        val showDuration = spell.growth.enabled && spell.growth.rules.any { it.target == GrowthTarget.DURATION }
        options.forEach { column ->
            SlotChoice(spell, column, column.level == chosen, showDuration) { chosen = column.level }
        }
        AppButton(
            text = stringResource(Res.string.me_spells_apply_button, current.level.toRomanNumeral()),
            onClick = { send(SpellsEvent.Cast(spell.id, SpellPay.Slot(current.level))) },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Solid,
            enabled = current.resource.current > 0,
            height = 52.dp,
        )
    }
}

@Composable
private fun SlotChoice(
    spell: CharacterSpell,
    column: SlotColumn,
    on: Boolean,
    showDuration: Boolean,
    onClick: () -> Unit,
) {
    val free = column.resource.current
    val at = spell.atCircle(column.level)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (free > 0) 1f else DIMMED)
            .appCard(
                AppShapes.Field,
                if (on) AppPalette.SurfaceElevated else AppPalette.Background,
                if (on) AppPalette.Arcane else AppPalette.BorderSubtle,
                onClick = onClick,
            ).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            column.level.toRomanNumeral(),
            Modifier.width(CircleWidth),
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.Arcane,
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DamageLine(at.damage, at.projectiles, AppText.footnote)
            if (showDuration) {
                val changed = at.durationUnit != spell.durationUnit || at.durationValue != spell.durationValue
                Text(
                    "◎ " + durationLabel(at.durationUnit, at.durationValue),
                    style = AppText.footnote,
                    color = if (changed) AppPalette.GoldBright else AppPalette.TextSecondary,
                )
            }
        }
        Text(
            if (free > 0) {
                stringResource(Res.string.me_spells_apply_free, free, column.resource.max)
            } else {
                stringResource(Res.string.me_spells_apply_none)
            },
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
    }
}

private const val DIMMED = 0.55f
