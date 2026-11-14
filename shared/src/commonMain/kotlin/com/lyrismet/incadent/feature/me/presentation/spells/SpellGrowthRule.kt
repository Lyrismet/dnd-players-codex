package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppRemoveButton
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.GROWTH_EVERY_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.GrowthMode
import com.lyrismet.incadent.feature.codex.domain.model.GrowthRule
import com.lyrismet.incadent.feature.codex.domain.model.GrowthStep
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.MAX_DURATION_VALUE
import com.lyrismet.incadent.feature.codex.domain.model.MAX_GROWTH_STEPS
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.model.durationStep
import com.lyrismet.incadent.feature.codex.domain.model.isValidDice
import com.lyrismet.incadent.feature.codex.domain.model.parseDurationStep
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_damage_dice_hint
import incadent.shared.generated.resources.me_spells_growth_add
import incadent.shared.generated.resources.me_spells_growth_add_step
import incadent.shared.generated.resources.me_spells_growth_circle
import incadent.shared.generated.resources.me_spells_growth_every_n
import incadent.shared.generated.resources.me_spells_growth_from
import incadent.shared.generated.resources.me_spells_growth_mode_every
import incadent.shared.generated.resources.me_spells_growth_mode_every_n
import incadent.shared.generated.resources.me_spells_growth_mode_steps
import incadent.shared.generated.resources.me_spells_growth_value_hint
import incadent.shared.generated.resources.me_spells_step_less
import incadent.shared.generated.resources.me_spells_step_more
import org.jetbrains.compose.resources.stringResource

private const val FIELD_MAX = 16
private val DiceFieldWidth = 120.dp

/** one rule: what grows, how it grows and the amount, or the table of steps */
@Composable
internal fun GrowthRuleCard(
    spell: CharacterSpell,
    rule: GrowthRule,
    onChange: (GrowthRule) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(
                background = AppPalette.Background,
                border = AppPalette.BorderSubtle,
            ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                growthTargetLabel(rule.target),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.TextPrimary,
            )
            AppRemoveButton(onClick = onRemove, size = 36.dp)
        }
        if (rule.target == GrowthTarget.DICE && spell.damage.size > 1) DamageRowPicker(spell, rule, onChange)
        if (!rule.stepsOnly) ModeTabs(rule, onChange)
        when (rule.mode) {
            GrowthMode.EVERY -> AmountField(rule, spell) { onChange(rule.copy(add = it)) }
            GrowthMode.EVERY_N -> {
                EveryN(rule, onChange)
                AmountField(rule, spell) { onChange(rule.copy(add = it)) }
            }
            GrowthMode.STEPS -> StepsTable(spell, rule, onChange)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DamageRowPicker(
    spell: CharacterSpell,
    rule: GrowthRule,
    onChange: (GrowthRule) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        spell.damage.forEachIndexed { index, row ->
            SelectablePill(
                "${row.dice} ${effectLabel(row.effect)}".trim(),
                rule.damageIndex == index,
                { onChange(rule.copy(damageIndex = index)) },
                size = PillSize.Small,
            )
        }
    }
}

@Composable
private fun ModeTabs(
    rule: GrowthRule,
    onChange: (GrowthRule) -> Unit,
) {
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(GrowthMode.EVERY, stringResource(Res.string.me_spells_growth_mode_every)),
                SegmentedTab(GrowthMode.EVERY_N, stringResource(Res.string.me_spells_growth_mode_every_n)),
                SegmentedTab(GrowthMode.STEPS, stringResource(Res.string.me_spells_growth_mode_steps)),
            ),
        selected = rule.mode,
        onSelected = { onChange(rule.copy(mode = it)) },
        modifier = Modifier.fillMaxWidth(),
        containerBackground = AppPalette.Surface,
    )
}

@Composable
private fun EveryN(
    rule: GrowthRule,
    onChange: (GrowthRule) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.me_spells_growth_every_n),
            Modifier.weight(1f),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        Stepper(rule.every.toString(), {
            onChange(rule.copy(every = (rule.every - 1).coerceAtLeast(GROWTH_EVERY_RANGE.first)))
        }) {
            onChange(rule.copy(every = (rule.every + 1).coerceAtMost(GROWTH_EVERY_RANGE.last)))
        }
    }
}

@Composable
private fun Stepper(
    text: String,
    onLess: () -> Unit,
    onMore: () -> Unit,
) {
    AppStepper(
        count = 2,
        onDecrease = onLess,
        onIncrease = onMore,
        decreaseDescription = stringResource(Res.string.me_spells_step_less),
        increaseDescription = stringResource(Res.string.me_spells_step_more),
        tiles = true,
        removesAtOne = false,
        countText = text,
    )
}

/** what a rule adds on each circle: dice for a dice rule, a number for the others */
@Composable
private fun AmountField(
    rule: GrowthRule,
    spell: CharacterSpell,
    onAdd: (String) -> Unit,
) {
    val label = stringResource(Res.string.me_spells_growth_add)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = AppText.footnote, color = AppPalette.TextSecondary)
        if (rule.target == GrowthTarget.DICE) {
            FormInlineField(
                value = rule.add,
                onChange = onAdd,
                modifier = Modifier.width(DiceFieldWidth),
                placeholder = stringResource(Res.string.me_spells_damage_dice_hint),
                maxLength = FIELD_MAX,
                invalid = !isValidDice(rule.add),
                counter = false,
            )
        } else {
            val n = rule.add.toIntOrNull() ?: 1
            Stepper("+$n", {
                onAdd((n - 1).coerceAtLeast(1).toString())
            }) { onAdd((n + 1).coerceAtMost(MAX_DURATION_VALUE).toString()) }
        }
    }
    if (rule.target ==
        GrowthTarget.DURATION
    ) {
        Text(durationLabel(spell.durationUnit, 1), style = AppText.footnote, color = AppPalette.TextTertiary)
    }
}

@Composable
private fun StepsTable(
    spell: CharacterSpell,
    rule: GrowthRule,
    onChange: (GrowthRule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.me_spells_growth_circle, spell.level.toRomanNumeral()) + " · " +
                baseValue(spell, rule),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        rule.steps.forEachIndexed { index, step ->
            StepRow(
                spell,
                rule,
                step,
                onStep = { next ->
                    onChange(rule.copy(steps = rule.steps.replaceAt(index) { next }))
                },
                onRemove = { onChange(rule.copy(steps = rule.steps.removeAt(index))) },
            )
        }
        if (rule.steps.size < MAX_GROWTH_STEPS) {
            SpellAddTile(stringResource(Res.string.me_spells_growth_add_step)) {
                val from = ((rule.steps.lastOrNull()?.from ?: spell.level) + 1).coerceAtMost(SPELL_LEVELS.last)
                onChange(rule.copy(steps = rule.steps + GrowthStep(from, newStepValue(rule))))
            }
        }
    }
}

private fun newStepValue(rule: GrowthRule): String =
    when (rule.target) {
        GrowthTarget.PROJECTILES, GrowthTarget.TARGETS -> "2"
        GrowthTarget.DURATION -> durationStep(DurationUnit.HOUR, 1)
        else -> ""
    }

@Composable
private fun baseValue(
    spell: CharacterSpell,
    rule: GrowthRule,
): String =
    when (rule.target) {
        GrowthTarget.DICE ->
            spell.damage
                .getOrNull(rule.damageIndex)
                ?.dice
                .orEmpty()
        GrowthTarget.PROJECTILES -> spell.projectiles.toString()
        GrowthTarget.DURATION -> durationLabel(spell)
        GrowthTarget.TARGETS -> "0"
        GrowthTarget.RANGE -> spell.range
        GrowthTarget.CUSTOM -> ""
    }

@Composable
private fun StepRow(
    spell: CharacterSpell,
    rule: GrowthRule,
    step: GrowthStep,
    onStep: (GrowthStep) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.me_spells_growth_from, step.from.toRomanNumeral()),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextPrimary,
            )
            Stepper(
                step.from.toRomanNumeral(),
                { onStep(step.copy(from = (step.from - 1).coerceAtLeast(spell.level + 1))) },
            ) { onStep(step.copy(from = (step.from + 1).coerceAtMost(SPELL_LEVELS.last))) }
            AppRemoveButton(onClick = onRemove, size = 36.dp)
        }
        StepValue(rule, step.value) { onStep(step.copy(value = it)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepValue(
    rule: GrowthRule,
    value: String,
    onValue: (String) -> Unit,
) {
    when (rule.target) {
        GrowthTarget.DICE ->
            FormInlineField(
                value = value,
                onChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.me_spells_damage_dice_hint),
                maxLength = FIELD_MAX,
                invalid = !isValidDice(value),
                counter = false,
            )
        GrowthTarget.PROJECTILES, GrowthTarget.TARGETS -> {
            val n = value.toIntOrNull() ?: 1
            Stepper(n.toString(), { onValue((n - 1).coerceAtLeast(1).toString()) }) { onValue((n + 1).toString()) }
        }
        GrowthTarget.DURATION -> {
            val (unit, n) = parseDurationStep(value) ?: (DurationUnit.HOUR to 1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Stepper(n.toString(), { onValue(durationStep(unit, (n - 1).coerceAtLeast(1))) }) {
                    onValue(durationStep(unit, (n + 1).coerceAtMost(MAX_DURATION_VALUE)))
                }
                DurationUnitPills(unit) { onValue(durationStep(it, n)) }
            }
        }
        GrowthTarget.RANGE, GrowthTarget.CUSTOM ->
            FormInlineField(
                value = value,
                onChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.me_spells_growth_value_hint),
            )
    }
}
