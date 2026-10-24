package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide

/** the short lines under the table, each is left out when there is nothing to say */
data class Highlights(
    // the party member who dealt the most, and how much
    val topDamage: Pair<String, Int>?,
    // those who put enemies down, once each
    val finishers: List<String>,
    // who went down and how many times
    val falls: List<Pair<String, Int>>,
) {
    val isEmpty: Boolean get() = topDamage == null && finishers.isEmpty() && falls.isEmpty()
}

fun highlightsOf(combat: Combat): Highlights {
    val rows = resultRows(combat)
    val top = rows.filter { it.side == CombatSide.PARTY }.maxByOrNull { it.dealt }?.takeIf { it.dealt > 0 }
    val downs = combat.events.filter { it.kind == CombatEventKind.DOWN }
    val byId = combat.combatants.associateBy { it.id }
    val finishers =
        downs
            .filter { byId[it.targetId]?.side != CombatSide.PARTY && it.sourceId != null && it.sourceId != it.targetId }
            .mapNotNull { byId[it.sourceId]?.name }
            .distinct()
    val falls =
        downs
            .groupBy { it.targetId }
            .mapNotNull { (id, list) -> byId[id]?.let { it.name to list.size } }
    return Highlights(top?.let { it.name to it.dealt }, finishers, falls)
}
