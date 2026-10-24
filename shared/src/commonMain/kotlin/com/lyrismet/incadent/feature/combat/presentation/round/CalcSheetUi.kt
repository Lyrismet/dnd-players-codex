package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.NumberPadKeyGrid
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_done_title
import dndplayerscodex.shared.generated.resources.combat_calc_done_value
import dndplayerscodex.shared.generated.resources.combat_calc_mode_damage
import dndplayerscodex.shared.generated.resources.combat_calc_mode_heal
import dndplayerscodex.shared.generated.resources.combat_calc_mode_temp
import dndplayerscodex.shared.generated.resources.combat_calc_more
import dndplayerscodex.shared.generated.resources.combat_calc_source_title
import org.jetbrains.compose.resources.stringResource

private const val DAMAGE_BORDER_ALPHA = 0.6f
private const val BORDER_ALPHA = 0.5f
private val DamageKeyHeight = 42.dp
private val ValueKeyHeight = 54.dp

/** the colours a mode paints the sheet with: its text, the fill of the apply button and what is written on it */
private class ModeAccent(
    val text: Color,
    val border: Color,
    val fill: Color,
    val onFill: Color,
)

private fun accentOf(mode: CalcMode): ModeAccent =
    when (mode) {
        CalcMode.DAMAGE ->
            ModeAccent(
                AppPalette.MaroonBright,
                AppPalette.Maroon.copy(alpha = DAMAGE_BORDER_ALPHA),
                AppPalette.Maroon,
                Color.White,
            )
        CalcMode.HEAL ->
            ModeAccent(
                AppPalette.EmeraldBright,
                AppPalette.Emerald.copy(alpha = BORDER_ALPHA),
                AppPalette.Emerald,
                AppPalette.HealOnFill,
            )
        CalcMode.TEMP ->
            ModeAccent(
                AppPalette.Azure,
                AppPalette.Azure.copy(alpha = BORDER_ALPHA),
                AppPalette.Azure,
                AppPalette.TempOnFill,
            )
    }

/** the damage and healing calculator: a sum of typed numbers dealt to one or several targets */
@Composable
internal fun CalcSheet(
    calc: CalcState,
    send: (CombatRoundEvent) -> Unit,
) {
    val emit = { event: CalcEvent -> send(CombatRoundEvent.Calc(event)) }
    val accent = accentOf(calc.mode)
    val damage = calc.mode == CalcMode.DAMAGE
    AppBottomSheet(onDismissRequest = { emit(CalcEvent.Closed) }) {
        AppSheetColumn(scroll = true, spacing = 12.dp) {
            CalcHeader(calc, accent, emit)
            if (calc.modes.size > 1) CalcModes(calc, emit)
            CalcSourceChips(calc, emit)
            if (calc.done.isNotEmpty()) DoneBlock(calc.done)
            TargetsBlock(calc, emit)
            if (damage) TypeBlock(calc, emit)
            ResultBlock(calc, emit)
            if (calc.keysShown) {
                NumberPadKeyGrid(
                    onDigit = { emit(CalcEvent.Digit(it)) },
                    onBackspace = { emit(CalcEvent.Backspace) },
                    onClear = { emit(CalcEvent.Clear) },
                    onPlus = if (damage) ({ emit(CalcEvent.Plus) }) else null,
                    keyHeight = if (damage) DamageKeyHeight else ValueKeyHeight,
                )
            }
            ApplyRow(calc, accent, emit)
        }
    }
}

@Composable
private fun CalcHeader(
    calc: CalcState,
    accent: ModeAccent,
    emit: (CalcEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = calc.overline,
                color = accent.text,
                fontSize = AppFontSize.Caption,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = calc.title,
                style = MaterialTheme.typography.headlineMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SheetCloseButton(onClick = { emit(CalcEvent.Closed) }, size = 40.dp)
    }
}

@Composable
private fun CalcModes(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    AppSegmentedTabs(
        tabs =
            calc.modes.map {
                SegmentedTab(
                    it,
                    stringResource(
                        when (it) {
                            CalcMode.DAMAGE -> Res.string.combat_calc_mode_damage
                            CalcMode.HEAL -> Res.string.combat_calc_mode_heal
                            CalcMode.TEMP -> Res.string.combat_calc_mode_temp
                        },
                    ),
                )
            },
        selected = calc.mode,
        onSelected = { emit(CalcEvent.ModeSelected(it)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CalcSourceChips(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormFieldLabel(stringResource(Res.string.combat_calc_source_title), Modifier.weight(1f))
            Text(calc.sourceHint, color = AppPalette.TextSecondary, fontSize = AppFontSize.Caption)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            calc.sources.forEach { chip ->
                val tint = RoundColors.side(chip.side)
                Text(
                    text = chip.label,
                    color = if (chip.selected) tint else AppPalette.TextSecondary,
                    fontSize = AppFontSize.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .appCard(
                                shape = AppShapes.Pill,
                                background = if (chip.selected) tint.copy(alpha = 0.14f) else Color.Transparent,
                                border = if (chip.selected) tint.copy(alpha = 0.5f) else AppPalette.Border,
                                onClick = { emit(CalcEvent.SourceSelected(chip.id)) },
                            ).padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DoneBlock(done: List<DoneHit>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Field, background = AppPalette.Background, border = AppPalette.BorderSubtle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormFieldLabel(stringResource(Res.string.combat_calc_done_title))
        done.forEach {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    it.label,
                    color = AppPalette.TextDescription,
                    fontSize = AppFontSize.Body,
                    lineHeight = AppLineHeight.Row,
                    maxLines = 1,
                )
                Text(
                    stringResource(Res.string.combat_calc_done_value, it.damage),
                    color = AppPalette.MaroonBright,
                    fontSize = AppFontSize.Body,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ApplyRow(
    calc: CalcState,
    accent: ModeAccent,
    emit: (CalcEvent) -> Unit,
) {
    val apply = @Composable { modifier: Modifier ->
        CalcButton(
            calc.applyLabel,
            calc.applyEnabled,
            accent.fill,
            accent.onFill,
            null,
            modifier,
        ) { emit(CalcEvent.Applied) }
    }
    if (calc.mode == CalcMode.DAMAGE) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton(
                text = stringResource(Res.string.combat_calc_more),
                enabled = calc.canMore,
                fill = Color.Transparent,
                onFill = if (calc.canMore) AppPalette.TextPrimary else AppPalette.TextTertiary,
                border = AppPalette.BorderHover,
                modifier = Modifier.weight(1f),
            ) { emit(CalcEvent.MoreHit) }
            apply(Modifier.weight(1f))
        }
    } else {
        apply(Modifier.fillMaxWidth())
    }
}

@Composable
private fun CalcButton(
    text: String,
    enabled: Boolean,
    fill: Color,
    onFill: Color,
    border: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 50.dp)
                .appCard(
                    shape = AppShapes.Card,
                    background = if (enabled || border != null) fill else AppPalette.SurfaceElevated,
                    border = border,
                    onClick = if (enabled) onClick else null,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled || border != null) onFill else AppPalette.TextTertiary,
            fontSize = AppFontSize.MediumPlus,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
