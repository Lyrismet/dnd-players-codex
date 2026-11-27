package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.SpellCondition
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_condition_advantage
import incadent.shared.generated.resources.me_spells_condition_blinded
import incadent.shared.generated.resources.me_spells_condition_charmed
import incadent.shared.generated.resources.me_spells_condition_deafened
import incadent.shared.generated.resources.me_spells_condition_disadvantage
import incadent.shared.generated.resources.me_spells_condition_exhaustion
import incadent.shared.generated.resources.me_spells_condition_frightened
import incadent.shared.generated.resources.me_spells_condition_grappled
import incadent.shared.generated.resources.me_spells_condition_incapacitated
import incadent.shared.generated.resources.me_spells_condition_invisible
import incadent.shared.generated.resources.me_spells_condition_paralyzed
import incadent.shared.generated.resources.me_spells_condition_petrified
import incadent.shared.generated.resources.me_spells_condition_poisoned
import incadent.shared.generated.resources.me_spells_condition_prone
import incadent.shared.generated.resources.me_spells_condition_restrained
import incadent.shared.generated.resources.me_spells_condition_stunned
import incadent.shared.generated.resources.me_spells_condition_unconscious
import incadent.shared.generated.resources.me_spells_dur_day
import incadent.shared.generated.resources.me_spells_dur_hour
import incadent.shared.generated.resources.me_spells_dur_min
import incadent.shared.generated.resources.me_spells_dur_rounds
import incadent.shared.generated.resources.me_spells_duration_instant
import incadent.shared.generated.resources.me_spells_effect_healing
import incadent.shared.generated.resources.me_spells_effect_reduction
import incadent.shared.generated.resources.me_spells_effect_temp_hp
import incadent.shared.generated.resources.me_spells_kind_action
import incadent.shared.generated.resources.me_spells_kind_bonus
import incadent.shared.generated.resources.me_spells_kind_long
import incadent.shared.generated.resources.me_spells_kind_reaction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun castKindLabel(kind: CastKind): String =
    stringResource(
        when (kind) {
            CastKind.ACTION -> Res.string.me_spells_kind_action
            CastKind.BONUS -> Res.string.me_spells_kind_bonus
            CastKind.REACTION -> Res.string.me_spells_kind_reaction
            CastKind.LONG -> Res.string.me_spells_kind_long
        },
    )

/** the casting time as the card writes it - the kind, or the own time of a long one */
@Composable
internal fun castTimeLabel(spell: CharacterSpell): String =
    if (spell.castKind == CastKind.LONG && spell.castingTime.isNotBlank()) {
        spell.castingTime
    } else {
        castKindLabel(spell.castKind)
    }

/** a duration with its number and unit, empty for a spell that has none set */
@Composable
internal fun durationLabel(
    unit: DurationUnit,
    value: Int,
    legacy: String = "",
): String =
    when (unit) {
        DurationUnit.NONE -> legacy
        DurationUnit.INSTANT -> stringResource(Res.string.me_spells_duration_instant)
        DurationUnit.ROUND -> pluralStringResource(Res.plurals.me_spells_dur_rounds, value, value)
        DurationUnit.MINUTE -> stringResource(Res.string.me_spells_dur_min, value)
        DurationUnit.HOUR -> stringResource(Res.string.me_spells_dur_hour, value)
        DurationUnit.DAY -> stringResource(Res.string.me_spells_dur_day, value)
    }

@Composable
internal fun durationLabel(spell: CharacterSpell): String =
    durationLabel(spell.durationUnit, spell.durationValue, spell.duration)

@Composable
internal fun effectLabel(effect: SpellEffect?): String =
    when (effect) {
        null -> ""
        SpellEffect.Healing -> stringResource(Res.string.me_spells_effect_healing)
        SpellEffect.TempHp -> stringResource(Res.string.me_spells_effect_temp_hp)
        SpellEffect.Reduction -> stringResource(Res.string.me_spells_effect_reduction)
        is SpellEffect.Condition -> stringResource(effect.kind.label())
        is SpellEffect.Damage -> damageTypeLabels()[effect.type].orEmpty()
    }

/** the damage rows in one line, an icon before the dice of each and the projectile count in front, "3 × 1d4+1" */
@Composable
internal fun DamageLine(
    damage: List<SpellDamage>,
    projectiles: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.TextPrimary,
) {
    val rows = damage.filter { it.dice.isNotBlank() }
    if (rows.isEmpty()) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Text("+", style = style, color = AppPalette.TextTertiary)
            SpellEffectGlyph(row.effect)
            // the projectile count multiplies all the rows, it is told once before the first dice
            val text = if (index == 0 && projectiles > 1) "$projectiles × ${row.dice}" else row.dice
            Text(text, style = style, color = color, maxLines = 1)
        }
    }
}

/** the name of the effect with a capital, for a field and a pick list where it stands alone */
@Composable
internal fun effectTitle(effect: SpellEffect?): String = effectLabel(effect).replaceFirstChar { it.uppercase() }

private val ConditionLabels: Map<SpellCondition, StringResource> =
    mapOf(
        SpellCondition.ADVANTAGE to Res.string.me_spells_condition_advantage,
        SpellCondition.DISADVANTAGE to Res.string.me_spells_condition_disadvantage,
        SpellCondition.BLINDED to Res.string.me_spells_condition_blinded,
        SpellCondition.CHARMED to Res.string.me_spells_condition_charmed,
        SpellCondition.DEAFENED to Res.string.me_spells_condition_deafened,
        SpellCondition.EXHAUSTION to Res.string.me_spells_condition_exhaustion,
        SpellCondition.FRIGHTENED to Res.string.me_spells_condition_frightened,
        SpellCondition.GRAPPLED to Res.string.me_spells_condition_grappled,
        SpellCondition.INCAPACITATED to Res.string.me_spells_condition_incapacitated,
        SpellCondition.INVISIBLE to Res.string.me_spells_condition_invisible,
        SpellCondition.PARALYZED to Res.string.me_spells_condition_paralyzed,
        SpellCondition.PETRIFIED to Res.string.me_spells_condition_petrified,
        SpellCondition.POISONED to Res.string.me_spells_condition_poisoned,
        SpellCondition.PRONE to Res.string.me_spells_condition_prone,
        SpellCondition.RESTRAINED to Res.string.me_spells_condition_restrained,
        SpellCondition.STUNNED to Res.string.me_spells_condition_stunned,
        SpellCondition.UNCONSCIOUS to Res.string.me_spells_condition_unconscious,
    )

private fun SpellCondition.label(): StringResource = ConditionLabels.getValue(this)
