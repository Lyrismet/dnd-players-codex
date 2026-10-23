package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecastInput
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.forecast.StateMark
import com.lyrismet.incadent.feature.combat.domain.forecast.forecastHits
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_round_left
import dndplayerscodex.shared.generated.resources.combat_round_more_than
import dndplayerscodex.shared.generated.resources.combat_round_short_left
import dndplayerscodex.shared.generated.resources.combat_round_short_more
import dndplayerscodex.shared.generated.resources.combat_round_took
import org.jetbrains.compose.resources.stringResource

internal fun Combat.damageTaken(member: Combatant): Int =
    events.filter { it.kind == CombatEventKind.DAMAGE && it.targetId == member.id }.sumOf { it.amount }

/** the damage taken at the last "bloodied" or "barely standing" mark, the mark is a snapshot */
internal fun Combat.markAt(member: Combatant): Int? =
    events
        .lastOrNull { it.targetId == member.id && it.kind == CombatEventKind.STATE && it.state == member.state }
        ?.amount
        ?.takeIf { member.state == CombatState.BLOODIED || member.state == CombatState.BARELY_STANDING }

/** what is left of an enemy whose hit points are only guessed: facts first, then the master's words */
internal fun Combat.forecast(
    member: Combatant,
    seen: List<SpeciesHitObservation> = emptyList(),
): HitForecast {
    if (member.side == CombatSide.PARTY || member.isTracked() || member.isDown()) return HitForecast.Unknown
    val marks =
        events
            .filter { it.targetId == member.id && it.kind == CombatEventKind.STATE && it.state != null }
            .map { StateMark(checkNotNull(it.state), it.amount) }
    return forecastHits(HitForecastInput(damageTaken(member), member.enteredWhole, marks, seen))
}

@Composable
internal fun HitForecast.shortText(): String? =
    when (this) {
        is HitForecast.Estimate -> stringResource(Res.string.combat_round_short_left, remaining)
        is HitForecast.AtLeast -> stringResource(Res.string.combat_round_short_more, damage)
        HitForecast.Unknown -> null
    }

@Composable
internal fun HitForecast.longText(): String? =
    when (this) {
        is HitForecast.Estimate -> stringResource(Res.string.combat_round_left, remaining)
        is HitForecast.AtLeast -> stringResource(Res.string.combat_round_more_than, damage)
        HitForecast.Unknown -> null
    }

@Composable
internal fun tookText(taken: Int): String = stringResource(Res.string.combat_round_took, taken)
