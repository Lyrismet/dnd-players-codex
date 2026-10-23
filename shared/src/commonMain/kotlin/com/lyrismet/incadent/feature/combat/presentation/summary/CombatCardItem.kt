package com.lyrismet.incadent.feature.combat.presentation.summary

import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.feature.combat.domain.grouping.enemyTitle
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.results.minutesUntil
import kotlinx.datetime.LocalDateTime

/** a finished fight as the session feed shows it */
data class CombatCardItem(
    val id: Long,
    val finishedAt: LocalDateTime,
    val timeLabel: String,
    val rounds: Int,
    val minutes: Int,
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
                minutes = minutesUntil(end),
                title = combatants.enemyTitle(),
                outcome = result,
                summary = summaryOf(this),
            )
        }
    }
