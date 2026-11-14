package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_dur_day
import incadent.shared.generated.resources.me_spells_dur_hour
import incadent.shared.generated.resources.me_spells_dur_min
import incadent.shared.generated.resources.me_spells_dur_rounds
import incadent.shared.generated.resources.me_spells_duration_instant
import incadent.shared.generated.resources.me_spells_effect_healing
import incadent.shared.generated.resources.me_spells_effect_temp_hp
import incadent.shared.generated.resources.me_spells_kind_action
import incadent.shared.generated.resources.me_spells_kind_bonus
import incadent.shared.generated.resources.me_spells_kind_long
import incadent.shared.generated.resources.me_spells_kind_reaction
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

/** the casting time as the card writes it: the kind, or the own time of a long one */
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
        is SpellEffect.Damage -> damageTypeLabels()[effect.type].orEmpty()
    }

/** the damage rows in one line, an icon before the dice of each and the projectile count in front, "3 × 1d4+1" */
@Composable
internal fun DamageLine(
    damage: List<SpellDamage>,
    projectiles: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val rows = damage.filter { it.dice.isNotBlank() }
    if (rows.isEmpty()) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Text("+", style = style, color = AppPalette.TextTertiary)
            SpellEffectGlyph(row.effect)
            // the projectile count multiplies all the rows, it is told once before the first dice
            val text = if (index == 0 && projectiles > 1) "$projectiles × ${row.dice}" else row.dice
            Text(text, style = style, color = AppPalette.TextPrimary, maxLines = 1)
        }
    }
}

/** the name of the effect with a capital, for a field and a pick list where it stands alone */
@Composable
internal fun effectTitle(effect: SpellEffect?): String = effectLabel(effect).replaceFirstChar { it.uppercase() }
