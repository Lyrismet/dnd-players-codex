package com.lyrismet.incadent.feature.combat.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.results.FightDuration
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_results_minutes
import incadent.shared.generated.resources.combat_results_seconds
import org.jetbrains.compose.resources.stringResource

/** "24 с" for a short fight, "≈ 1 мин" for a longer one */
@Composable
fun fightDurationText(duration: FightDuration): String =
    when (duration) {
        is FightDuration.Seconds -> stringResource(Res.string.combat_results_seconds, duration.value)
        is FightDuration.AboutMinutes -> stringResource(Res.string.combat_results_minutes, duration.value)
    }
