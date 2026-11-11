package com.lyrismet.incadent.feature.combat.presentation.summary

import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.feature.combat.domain.grouping.enemyTitle
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.results.FightDuration
import com.lyrismet.incadent.feature.combat.domain.results.fightDuration
import kotlinx.datetime.LocalDateTime

/** a finished fight as the session feed shows it */
data class CombatCardItem(
    val id: Long,
    val finishedAt: LocalDateTime,
    val timeLabel: String,
    val rounds: Int,
    val duration: FightDuration,
    val title: String,
    val outcome: CombatOutcome,
    val summary: CombatSummary,
)

/** the fight as a card, none for one that is not finished */
fun Combat.toCardItem(): CombatCardItem? =
    finishedAt?.let { end ->
        outcome?.let { result ->
            CombatCardItem(
                id = id,
                finishedAt = end,
                timeLabel = end.toDisplayTime(),
                rounds = round,
                duration = fightDuration(round),
                title = combatants.enemyTitle(),
                outcome = result,
                summary = summaryOf(this),
            )
        }
    }
