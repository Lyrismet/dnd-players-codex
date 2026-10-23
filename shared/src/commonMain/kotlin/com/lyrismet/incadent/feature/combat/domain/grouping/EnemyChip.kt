package com.lyrismet.incadent.feature.combat.domain.grouping

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** one kind of enemy in a journal row, [several] is set when it came as a numbered group ("Скелет-лучник ×2") */
data class EnemyChip(
    val name: String,
    val count: Int,
    val several: Boolean,
)

/** the enemies of a combat grouped by name without the autonumber, in the order they first appear */
fun List<Combatant>.enemyChips(): List<EnemyChip> {
    val groups = linkedMapOf<String, Pair<Int, Boolean>>()
    filter { it.side == CombatSide.ENEMY }.forEach { enemy ->
        val base = enemy.name.baseName()
        val (count, several) = groups[base] ?: (0 to false)
        groups[base] = (count + 1) to (several || base != enemy.name || count >= 1)
    }
    return groups.map { (name, group) -> EnemyChip(name, group.first, group.second) }
}

/** the enemy chips as one line, a group carries its count ("Скелет-лучник ×2") */
fun List<Combatant>.enemyTitle(): String =
    enemyChips().joinToString(", ") {
        if (it.count >
            1
        ) {
            "${it.name} ×${it.count}"
        } else {
            it.name
        }
    }
