package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.lyrismet.incadent.core.designsystem.component.AppPickSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CantripGrowth
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.GrowthRule
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.MAX_GROWTH_RULES
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_damage_type_empty
import incadent.shared.generated.resources.me_spells_growth_add_rule
import incadent.shared.generated.resources.me_spells_growth_cantrip
import incadent.shared.generated.resources.me_spells_growth_cantrip_hint
import incadent.shared.generated.resources.me_spells_growth_custom
import incadent.shared.generated.resources.me_spells_growth_custom_hint
import incadent.shared.generated.resources.me_spells_growth_dice
import incadent.shared.generated.resources.me_spells_growth_dice_hint
import incadent.shared.generated.resources.me_spells_growth_duration
import incadent.shared.generated.resources.me_spells_growth_duration_hint
import incadent.shared.generated.resources.me_spells_growth_pick_title
import incadent.shared.generated.resources.me_spells_growth_projectiles
import incadent.shared.generated.resources.me_spells_growth_projectiles_hint
import incadent.shared.generated.resources.me_spells_growth_range
import incadent.shared.generated.resources.me_spells_growth_range_hint
import incadent.shared.generated.resources.me_spells_growth_spell
import incadent.shared.generated.resources.me_spells_growth_spell_hint
import incadent.shared.generated.resources.me_spells_growth_targets
import incadent.shared.generated.resources.me_spells_growth_targets_hint
import incadent.shared.generated.resources.me_spells_growth_title
import org.jetbrains.compose.resources.stringResource

private val CANTRIP_HINT = Res.string.me_spells_growth_cantrip_hint
private val SPELL_HINT = Res.string.me_spells_growth_spell_hint

@Composable
internal fun growthTargetLabel(target: GrowthTarget): String =
    stringResource(
        when (target) {
            GrowthTarget.DICE -> Res.string.me_spells_growth_dice
            GrowthTarget.PROJECTILES -> Res.string.me_spells_growth_projectiles
            GrowthTarget.DURATION -> Res.string.me_spells_growth_duration
            GrowthTarget.TARGETS -> Res.string.me_spells_growth_targets
            GrowthTarget.RANGE -> Res.string.me_spells_growth_range
            GrowthTarget.CUSTOM -> Res.string.me_spells_growth_custom
        },
    )

@Composable
private fun growthTargetHint(target: GrowthTarget): String =
    stringResource(
        when (target) {
            GrowthTarget.DICE -> Res.string.me_spells_growth_dice_hint
            GrowthTarget.PROJECTILES -> Res.string.me_spells_growth_projectiles_hint
            GrowthTarget.DURATION -> Res.string.me_spells_growth_duration_hint
            GrowthTarget.TARGETS -> Res.string.me_spells_growth_targets_hint
            GrowthTarget.RANGE -> Res.string.me_spells_growth_range_hint
            GrowthTarget.CUSTOM -> Res.string.me_spells_growth_custom_hint
        },
    )

/** the upcasting block of the form: a cantrip grows with the level, a spell by rules over the slot circle */
@Composable
internal fun GrowthFields(
    spell: CharacterSpell,
    characterLevel: Int,
    maxCircle: Int,
    change: (CharacterSpell) -> Unit,
) {
    val growth = spell.growth
    val on = if (spell.isCantrip) growth.cantrip != CantripGrowth.NONE else growth.enabled
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_growth_title))
        Row(
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Field, AppPalette.Background, if (on) AppPalette.Gold else AppPalette.BorderSubtle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (spell.isCantrip) Res.string.me_spells_growth_cantrip else Res.string.me_spells_growth_spell,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppPalette.TextPrimary,
                )
                Text(
                    stringResource(if (spell.isCantrip) CANTRIP_HINT else SPELL_HINT),
                    style = AppText.footnote,
                    color = AppPalette.TextSecondary,
                )
            }
            AppSwitch(on, { change(spell.copy(growth = toggled(spell, it))) })
        }
        if (on) {
            if (spell.isCantrip) {
                CantripBody(spell, characterLevel, change)
            } else {
                RulesBody(spell, characterLevel, maxCircle, change)
            }
        }
    }
}

// turning it on gives a first rule the spell can use, so the block is not an empty list
private fun toggled(
    spell: CharacterSpell,
    on: Boolean,
) = if (spell.isCantrip) {
    spell.growth.copy(cantrip = if (on) CantripGrowth.DICE else CantripGrowth.NONE)
} else if (on && spell.growth.rules.isEmpty()) {
    val target = if (spell.damage.isNotEmpty()) GrowthTarget.DICE else GrowthTarget.DURATION
    spell.growth.copy(enabled = true, rules = listOf(GrowthRule(target)))
} else {
    spell.growth.copy(enabled = on)
}

@Composable
private fun CantripBody(
    spell: CharacterSpell,
    characterLevel: Int,
    change: (CharacterSpell) -> Unit,
) {
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(CantripGrowth.DICE, stringResource(Res.string.me_spells_growth_dice)),
                SegmentedTab(CantripGrowth.PROJECTILES, stringResource(Res.string.me_spells_growth_projectiles)),
            ),
        selected = spell.growth.cantrip,
        onSelected = { change(spell.copy(growth = spell.growth.copy(cantrip = it))) },
        modifier = Modifier.fillMaxWidth(),
    )
    GrowthPreview(spell, characterLevel, maxCircle = 0)
}

@Composable
private fun RulesBody(
    spell: CharacterSpell,
    characterLevel: Int,
    maxCircle: Int,
    change: (CharacterSpell) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val rules = spell.growth.rules
    val setRules = { next: List<GrowthRule> -> change(spell.copy(growth = spell.growth.copy(rules = next))) }
    rules.forEachIndexed { index, rule ->
        GrowthRuleCard(
            spell,
            rule,
            onChange = { next -> setRules(rules.replaceAt(index) { next }) },
            onRemove = { setRules(rules.removeAt(index)) },
        )
    }
    if (rules.size < MAX_GROWTH_RULES) {
        SpellAddTile(stringResource(Res.string.me_spells_growth_add_rule)) { picking = true }
    }
    if (rules.isNotEmpty()) GrowthPreview(spell, characterLevel, maxCircle)
    if (picking) {
        TargetSheet(
            hasDamage = spell.damage.isNotEmpty(),
            onClose = { picking = false },
            onPick = {
                picking = false
                setRules(rules + GrowthRule(it))
            },
        )
    }
}

@Composable
private fun TargetSheet(
    hasDamage: Boolean,
    onClose: () -> Unit,
    onPick: (GrowthTarget) -> Unit,
) {
    // dice and projectiles have nothing to grow without a damage row
    val targets =
        GrowthTarget.entries.filter {
            hasDamage || (it != GrowthTarget.DICE && it != GrowthTarget.PROJECTILES)
        }
    AppPickSheet(
        items = targets,
        chosen = null,
        title = stringResource(Res.string.me_spells_growth_pick_title),
        emptyText = stringResource(Res.string.me_spells_damage_type_empty),
        label = { growthTargetLabel(it) },
        hint = { growthTargetHint(it) },
        onPick = { target -> target?.let(onPick) },
        onDismiss = onClose,
        showSearch = false,
    )
}
