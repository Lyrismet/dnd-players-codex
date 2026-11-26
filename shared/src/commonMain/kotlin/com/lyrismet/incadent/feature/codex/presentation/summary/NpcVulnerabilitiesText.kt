package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.feature.codex.domain.grouping.effectiveVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType

/** the features as plain lines, one per state and one for the facts, empty when there is nothing to copy */
internal fun NpcVulnerabilities.asText(
    typeLabels: Map<DamageType, String>,
    stateLabels: Map<DamageState, String>,
    factsLabel: String,
): String {
    val shieldLines =
        effectiveVulnerabilities(fromSpecies, own)
            .shields()
            .groupBy({ it.second }, { it.first })
            .map { (state, types) ->
                "${stateLabels.getValue(state)}: ${types.joinToString(", ") { typeLabels.getValue(it) }}"
            }
    val factsLine = facts.takeIf { it.isNotEmpty() }?.let { "$factsLabel: ${it.joinToString(", ")}" }
    return (shieldLines + listOfNotNull(factsLine)).joinToString("\n")
}
