package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.FormFieldHeight
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.formInputTextStyle
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_edit_range
import incadent.shared.generated.resources.me_spells_range_near
import incadent.shared.generated.resources.me_spells_range_own_hint
import incadent.shared.generated.resources.me_spells_range_self
import incadent.shared.generated.resources.me_spells_range_unit_ft
import incadent.shared.generated.resources.me_spells_range_unit_m
import org.jetbrains.compose.resources.stringResource

/** the common ranges as chips in the chosen unit and a number field with the unit switch beside it */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RangeFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    val feet = stringResource(Res.string.me_spells_range_unit_ft)
    val meters = stringResource(Res.string.me_spells_range_unit_m)
    val suffix = { unit: RangeUnit -> if (unit == RangeUnit.FEET) feet else meters }
    var unit by remember { mutableStateOf(rangeUnitOf(spell.range) ?: RangeUnit.FEET) }
    val common =
        listOf(stringResource(Res.string.me_spells_range_near), stringResource(Res.string.me_spells_range_self)) +
            unit.presets.map { "$it ${suffix(unit)}" }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_range))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            common.forEach { text ->
                SelectablePill(text, spell.range == text, { change(spell.copy(range = text)) }, size = PillSize.Small)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormInlineField(
                value = rangeNumberOf(spell.range)?.toString() ?: spell.range.takeIf { it !in common }.orEmpty(),
                onChange = { text ->
                    val digits = text.isNotEmpty() && text.all { it.isDigit() }
                    change(spell.copy(range = if (digits) "$text ${suffix(unit)}" else text))
                },
                modifier = Modifier.weight(1f - UNIT_WEIGHT),
                placeholder = stringResource(Res.string.me_spells_range_own_hint),
                counter = false,
            )
            val other = RangeUnit.entries.first { it != unit }
            Box(
                modifier =
                    Modifier
                        .weight(UNIT_WEIGHT)
                        .height(FormFieldHeight)
                        .appCard(
                            shape = AppShapes.Field,
                            background = AppPalette.Background,
                            border = AppPalette.Border,
                            onClick = {
                                unit = other
                                change(spell.copy(range = convertRange(spell.range, other, suffix)))
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) { Text(suffix(unit), style = formInputTextStyle(), color = AppPalette.TextPrimary) }
        }
    }
}

private const val UNIT_WEIGHT = 0.22f
