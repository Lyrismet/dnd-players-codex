package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.damage.hurt
import com.lyrismet.incadent.feature.combat.domain.damage.missingHp
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.calc_heal_to_max_full
import incadent.shared.generated.resources.calc_heal_to_max_many
import incadent.shared.generated.resources.calc_heal_to_max_one
import incadent.shared.generated.resources.calc_heal_to_max_temp_note
import incadent.shared.generated.resources.calc_heal_to_max_unknown
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun toMaxOf(targets: List<Combatant>): CalcToMax {
    val hurt = targets.hurt()
    val single = hurt.singleOrNull()
    val caption =
        when {
            single != null ->
                stringResource(
                    Res.string.calc_heal_to_max_one,
                    checkNotNull(single.hpMax),
                    checkNotNull(single.missingHp()),
                )
            hurt.size > 1 -> stringResource(Res.string.calc_heal_to_max_many, hurt.size)
            targets.any { it.hpMax != null } -> stringResource(Res.string.calc_heal_to_max_full)
            else -> stringResource(Res.string.calc_heal_to_max_unknown)
        }
    val note = if (hurt.any { it.tempHp > 0 }) stringResource(Res.string.calc_heal_to_max_temp_note) else null
    return CalcToMax(caption, enabled = hurt.isNotEmpty(), note = note)
}
