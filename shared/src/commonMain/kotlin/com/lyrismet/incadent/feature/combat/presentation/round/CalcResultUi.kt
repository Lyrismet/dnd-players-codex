package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.ShieldStyle
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_type_none
import org.jetbrains.compose.resources.stringResource

/** the thirteen damage types in a row, the chosen one is lit in its own colour */
@Composable
internal fun TypeBlock(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FormFieldLabel(calc.typeHeader)
            Text(calc.typeLabel, color = AppPalette.TextSecondary, fontSize = AppFontSize.Caption)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            calc.types.forEach { chip ->
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .appCard(
                                shape = AppShapes.Field,
                                background = if (chip.selected) AppPalette.SurfacePopover else Color.Transparent,
                                border = if (chip.selected) chip.type.shieldColor() else AppPalette.Border,
                                onClick = { emit(CalcEvent.TypeSelected(chip.type)) },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppDamageShield(
                        chip.type.shieldIconPath(),
                        chip.type.shieldColor(),
                        ShieldStyle.Plain,
                        width = 22.dp,
                    )
                }
            }
        }
    }
}

/** the typed sum with its types and the total, or the typed number with what it would do to the targets */
@Composable
internal fun ResultBlock(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    val damage = calc.mode == CalcMode.DAMAGE
    val tint = accentTint(calc.mode)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.Background,
                    border = if (damage) AppPalette.BorderHover else AppPalette.BorderSubtle,
                ).then(if (damage) Modifier.clickable { emit(CalcEvent.CollapseToggled) } else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (damage) {
            Terms(calc.terms, Modifier.weight(1f))
        } else {
            Column(modifier = Modifier.weight(1f)) {
                calc.preview.forEach {
                    Text(it, color = AppPalette.TextMuted, fontSize = AppFontSize.SmallPlus, lineHeight = LineHeight20)
                }
            }
        }
        Text(calc.result, color = tint, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (damage) {
            Text(
                if (calc.keysShown) "▾" else "▴",
                color = AppPalette.TextTertiary,
                fontSize = AppFontSize.Small,
            )
        }
    }
}

private fun accentTint(mode: CalcMode): Color =
    when (mode) {
        CalcMode.DAMAGE -> AppPalette.MaroonBright
        CalcMode.HEAL -> AppPalette.EmeraldBright
        CalcMode.TEMP -> AppPalette.Azure
    }

private val LineHeight20 = 20.sp

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Terms(
    terms: List<TermChip>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.defaultMinSize(minHeight = 34.dp),
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        terms.forEachIndexed { index, term ->
            if (index >
                0
            ) {
                Text(
                    "+",
                    color = AppPalette.TextTertiary,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
            Row(
                modifier =
                    Modifier
                        .appCard(
                            shape = AppShapes.Small,
                            background = if (term.current) AppPalette.SurfacePopover else Color.Transparent,
                            border = if (term.current) (term.type?.shieldColor() ?: AppPalette.RadioIdle) else null,
                        ).defaultMinSize(minHeight = 34.dp)
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(term.text, color = AppPalette.TextHeading, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                term.type?.let {
                    AppDamageShield(
                        it.shieldIconPath(),
                        it.shieldColor(),
                        ShieldStyle.Plain,
                        width = 16.dp,
                    )
                }
            }
        }
        if (terms.isEmpty()) {
            Text(
                stringResource(Res.string.combat_calc_type_none),
                color = Color.Transparent,
                fontSize = 24.sp,
            )
        }
    }
}
