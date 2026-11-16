package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_round_exact
import incadent.shared.generated.resources.combat_round_left
import incadent.shared.generated.resources.combat_round_more_than
import incadent.shared.generated.resources.combat_round_short_exact
import incadent.shared.generated.resources.combat_round_short_left
import incadent.shared.generated.resources.combat_round_short_more
import incadent.shared.generated.resources.combat_round_took
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HitForecast.shortText(): String? =
    when (this) {
        is HitForecast.Estimate -> stringResource(Res.string.combat_round_short_left, remaining)
        is HitForecast.AtLeast -> stringResource(Res.string.combat_round_short_more, damage)
        is HitForecast.Exact -> stringResource(Res.string.combat_round_short_exact, remaining)
        HitForecast.Unknown -> null
    }

@Composable
internal fun HitForecast.longText(): String? =
    when (this) {
        is HitForecast.Estimate -> stringResource(Res.string.combat_round_left, remaining)
        is HitForecast.AtLeast -> stringResource(Res.string.combat_round_more_than, damage)
        is HitForecast.Exact -> stringResource(Res.string.combat_round_exact, remaining)
        HitForecast.Unknown -> null
    }

@Composable
internal fun tookText(taken: Int): String = stringResource(Res.string.combat_round_took, taken)
