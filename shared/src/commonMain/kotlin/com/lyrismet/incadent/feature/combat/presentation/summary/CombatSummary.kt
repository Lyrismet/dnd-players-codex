package com.lyrismet.incadent.feature.combat.presentation.summary

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.results.Highlights
import com.lyrismet.incadent.feature.combat.domain.results.LogLine
import com.lyrismet.incadent.feature.combat.domain.results.ResultRow
import com.lyrismet.incadent.feature.combat.domain.results.highlightsOf
import com.lyrismet.incadent.feature.combat.domain.results.resultRows
import com.lyrismet.incadent.feature.combat.domain.results.roundLog

/** what the results of a fight say: the table, the highlights under it and the log by rounds */
data class CombatSummary(
    val rows: List<ResultRow>,
    val highlights: Highlights,
    val log: List<LogLine>,
)

fun summaryOf(combat: Combat): CombatSummary = CombatSummary(resultRows(combat), highlightsOf(combat), roundLog(combat))
