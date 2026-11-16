package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.forecast.ForecastSource
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import com.lyrismet.incadent.feature.combat.domain.forecast.HitScaleWords
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.forecast.ledgerOf
import com.lyrismet.incadent.feature.combat.domain.forecast.permilleOf
import com.lyrismet.incadent.feature.combat.domain.forecast.permilleOfExact
import com.lyrismet.incadent.feature.combat.domain.forecast.stateAtPermille
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.damageTaken
import com.lyrismet.incadent.feature.combat.domain.round.forecast
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.hp_exact_value
import incadent.shared.generated.resources.hp_scale_estimate_approx
import incadent.shared.generated.resources.hp_scale_estimate_at_least
import incadent.shared.generated.resources.hp_scale_estimate_exact
import incadent.shared.generated.resources.hp_scale_estimate_unknown
import incadent.shared.generated.resources.hp_scale_src_exact
import incadent.shared.generated.resources.hp_scale_src_scale
import incadent.shared.generated.resources.hp_scale_src_species
import incadent.shared.generated.resources.hp_scale_src_took
import org.jetbrains.compose.resources.stringResource

private const val FULL = 1000

/** the scale under "how many are left": where the thumb stands, the estimate with its source and what was typed */
@Composable
internal fun panelScale(
    combat: Combat,
    member: Combatant,
    seen: List<SpeciesHitObservation>,
): PanelScale {
    val forecast = combat.forecast(member, seen)
    val taken = combat.damageTaken(member)
    val thumb = scaleThumb(combat, member, forecast)
    val near = thumb?.let { place -> stateAtPermille(place) }
    val words =
        HitScaleWords.map { (state, permille) ->
            ScaleWord(stringResource(state.shortLabel()), permille, near = state == near)
        }
    return PanelScale(
        thumb = thumb,
        words = words,
        estimate = estimateText(forecast),
        source = sourceText(forecast, taken),
        exact = (forecast as? HitForecast.Exact)?.let { stringResource(Res.string.hp_exact_value, it.remaining) },
    )
}

/** how full the bars of a row are: where the slider stands, or the place of the word when it was never moved */
internal fun scaleFill(
    combat: Combat,
    member: Combatant,
    forecast: HitForecast,
): Float = ((scaleThumb(combat, member, forecast) ?: permilleOf(member.state) ?: 0) / FULL.toFloat()).coerceIn(0f, 1f)

private fun scaleThumb(
    combat: Combat,
    member: Combatant,
    forecast: HitForecast,
): Int? {
    if (forecast is HitForecast.Exact) return permilleOfExact(forecast.remaining, combat.ledgerOf(member.id).net)
    val last =
        combat.events.lastOrNull {
            it.targetId == member.id &&
                it.kind == CombatEventKind.STATE &&
                it.state != null
        }
    return when {
        last != null && last.raw > 0 -> last.raw
        last == null && member.state == CombatState.WHOLE -> null
        else -> permilleOf(member.state) ?: last?.state?.let { permilleOf(it) }
    }?.coerceIn(0, FULL)
}

@Composable
private fun estimateText(forecast: HitForecast): String =
    when (forecast) {
        is HitForecast.Exact -> stringResource(Res.string.hp_scale_estimate_exact, forecast.remaining)
        is HitForecast.Estimate -> stringResource(Res.string.hp_scale_estimate_approx, forecast.remaining)
        is HitForecast.AtLeast -> stringResource(Res.string.hp_scale_estimate_at_least, forecast.damage + 1)
        HitForecast.Unknown -> stringResource(Res.string.hp_scale_estimate_unknown)
    }

@Composable
private fun sourceText(
    forecast: HitForecast,
    taken: Int,
): String =
    when (forecast) {
        is HitForecast.Exact -> stringResource(Res.string.hp_scale_src_exact)
        is HitForecast.Estimate ->
            stringResource(
                if (forecast.source == ForecastSource.SPECIES) {
                    Res.string.hp_scale_src_species
                } else {
                    Res.string.hp_scale_src_scale
                },
                taken,
            )
        is HitForecast.AtLeast -> stringResource(Res.string.hp_scale_src_took, taken)
        HitForecast.Unknown -> ""
    }
