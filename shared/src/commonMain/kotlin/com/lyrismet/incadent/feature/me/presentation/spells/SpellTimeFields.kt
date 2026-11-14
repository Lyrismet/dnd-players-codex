package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.MAX_DURATION_VALUE
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_dur_rounds
import incadent.shared.generated.resources.me_spells_duration_10m
import incadent.shared.generated.resources.me_spells_duration_1m
import incadent.shared.generated.resources.me_spells_duration_combat
import incadent.shared.generated.resources.me_spells_duration_instant
import incadent.shared.generated.resources.me_spells_duration_no_combat
import incadent.shared.generated.resources.me_spells_edit_cast_time
import incadent.shared.generated.resources.me_spells_edit_duration
import incadent.shared.generated.resources.me_spells_long_10m
import incadent.shared.generated.resources.me_spells_long_1h
import incadent.shared.generated.resources.me_spells_long_1m
import incadent.shared.generated.resources.me_spells_long_own_hint
import incadent.shared.generated.resources.me_spells_other
import incadent.shared.generated.resources.me_spells_step_less
import incadent.shared.generated.resources.me_spells_step_more
import incadent.shared.generated.resources.me_spells_unit_day
import incadent.shared.generated.resources.me_spells_unit_hour
import incadent.shared.generated.resources.me_spells_unit_min
import incadent.shared.generated.resources.me_spells_unit_round
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val KindTileHeight = 46.dp
private const val MINUTE_ONE = 1
private const val MINUTE_TEN = 10

/** the four casting times as a grid of tiles with their shapes, a long one also asks for its own time */
@Composable
internal fun CastTimeFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_cast_time))
        CastKind.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    KindTile(kind, spell.castKind == kind, Modifier.weight(1f)) {
                        change(
                            spell.copy(
                                castKind = kind,
                                castingTime =
                                    if (kind ==
                                        CastKind.LONG
                                    ) {
                                        spell.castingTime
                                    } else {
                                        ""
                                    },
                            ),
                        )
                    }
                }
            }
        }
        if (spell.castKind == CastKind.LONG) LongTimeChips(spell, change)
    }
}

@Composable
private fun KindTile(
    kind: CastKind,
    on: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(KindTileHeight)
            .appCard(
                AppShapes.Field,
                if (on) AppPalette.SurfaceElevated else AppPalette.Background,
                if (on) AppPalette.Gold else AppPalette.BorderSubtle,
                onClick = onClick,
            ).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CastKindGlyph(kind)
        Text(
            castKindLabel(kind),
            style = MaterialTheme.typography.bodyMedium,
            color = if (on) AppPalette.GoldBright else AppPalette.TextPrimary,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LongTimeChips(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    val common =
        listOf(
            stringResource(Res.string.me_spells_long_1m),
            stringResource(Res.string.me_spells_long_10m),
            stringResource(Res.string.me_spells_long_1h),
        )
    var own by remember { mutableStateOf(spell.castingTime.isNotBlank() && spell.castingTime !in common) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        common.forEach { text ->
            SelectablePill(text, !own && spell.castingTime == text, {
                own = false
                change(spell.copy(castingTime = text))
            }, size = PillSize.Small)
        }
        SelectablePill(stringResource(Res.string.me_spells_other), own, { own = true }, size = PillSize.Small)
    }
    if (own) {
        FormInlineField(
            value = spell.castingTime.takeIf { it !in common }.orEmpty(),
            onChange = { change(spell.copy(castingTime = it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.me_spells_long_own_hint),
        )
    }
}

/** the duration as common chips and an own number with a unit, under it what the fight counts of it */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DurationFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    val unit = spell.durationUnit
    var own by remember {
        mutableStateOf(
            unit != DurationUnit.NONE && unit != DurationUnit.INSTANT && !spell.isCommonDuration(),
        )
    }
    val pick = {
        next: DurationUnit,
        value: Int,
        ->
        change(spell.copy(durationUnit = next, durationValue = value, duration = ""))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_duration))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectablePill(
                stringResource(
                    Res.string.me_spells_duration_instant,
                ),
                !own && unit == DurationUnit.INSTANT,
                {
                    own = false
                    pick(DurationUnit.INSTANT, 0)
                },
                size = PillSize.Small,
            )
            SelectablePill(stringResource(Res.string.me_spells_duration_1m), !own && spell.isMinutes(MINUTE_ONE), {
                own = false
                pick(DurationUnit.MINUTE, MINUTE_ONE)
            }, size = PillSize.Small)
            SelectablePill(stringResource(Res.string.me_spells_duration_10m), !own && spell.isMinutes(MINUTE_TEN), {
                own = false
                pick(DurationUnit.MINUTE, MINUTE_TEN)
            }, size = PillSize.Small)
            SelectablePill(stringResource(Res.string.me_spells_other), own, {
                own = true
                if (!spell.isCustomUnit()) pick(DurationUnit.HOUR, 1)
            }, size = PillSize.Small)
        }
        if (own) OwnDuration(spell, pick)
        CombatCaption(spell)
    }
}

private fun CharacterSpell.isMinutes(value: Int) = durationUnit == DurationUnit.MINUTE && durationValue == value

private fun CharacterSpell.isCommonDuration() = isMinutes(MINUTE_ONE) || isMinutes(MINUTE_TEN)

private fun CharacterSpell.isCustomUnit() = durationUnit != DurationUnit.NONE && durationUnit != DurationUnit.INSTANT

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnDuration(
    spell: CharacterSpell,
    pick: (DurationUnit, Int) -> Unit,
) {
    val unit = spell.durationUnit
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppStepper(
            count = spell.durationValue.coerceAtLeast(1),
            onDecrease = { pick(unit, (spell.durationValue - 1).coerceAtLeast(1)) },
            onIncrease = { pick(unit, (spell.durationValue + 1).coerceAtMost(MAX_DURATION_VALUE)) },
            decreaseDescription = stringResource(Res.string.me_spells_step_less),
            increaseDescription = stringResource(Res.string.me_spells_step_more),
            tiles = true,
            removesAtOne = false,
        )
        DurationUnitPills(unit) { pick(it, spell.durationValue.coerceAtLeast(1)) }
    }
}

/** the four units a duration is counted in as pills */
@Composable
internal fun DurationUnitPills(
    selected: DurationUnit,
    onPick: (DurationUnit) -> Unit,
) {
    val units =
        listOf(
            DurationUnit.ROUND to stringResource(Res.string.me_spells_unit_round),
            DurationUnit.MINUTE to stringResource(Res.string.me_spells_unit_min),
            DurationUnit.HOUR to stringResource(Res.string.me_spells_unit_hour),
            DurationUnit.DAY to stringResource(Res.string.me_spells_unit_day),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        units.forEach { (unit, label) ->
            SelectablePill(label, selected == unit, { onPick(unit) }, size = PillSize.Small)
        }
    }
}

@Composable
private fun CombatCaption(spell: CharacterSpell) {
    if (spell.durationUnit == DurationUnit.NONE || spell.durationUnit == DurationUnit.INSTANT) return
    val rounds = spell.combatRounds
    Text(
        if (rounds != null) {
            stringResource(
                Res.string.me_spells_duration_combat,
                pluralStringResource(Res.plurals.me_spells_dur_rounds, rounds, rounds),
            )
        } else {
            stringResource(Res.string.me_spells_duration_no_combat)
        },
        style = AppText.footnote,
        color = AppPalette.TextSecondary,
    )
}
