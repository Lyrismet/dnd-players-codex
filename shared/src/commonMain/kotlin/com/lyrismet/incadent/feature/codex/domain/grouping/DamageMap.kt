package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType

// a list row shows the strongest news first
private val SHIELD_ORDER = listOf(DamageState.VULNERABILITY, DamageState.IMMUNITY, DamageState.RESISTANCE)

fun Map<DamageType, DamageState>.stateOf(type: DamageType): DamageState = this[type] ?: DamageState.UNKNOWN

/** the map with [type] set to [state], an unknown state drops the entry */
fun Map<DamageType, DamageState>.with(
    type: DamageType,
    state: DamageState,
): Map<DamageType, DamageState> = if (state == DamageState.UNKNOWN) this - type else this + (type to state)

/** the types worth a shield: vulnerability, then immunity, then resistance, each in the catalog order */
fun Map<DamageType, DamageState>.shields(): List<Pair<DamageType, DamageState>> =
    SHIELD_ORDER.flatMap { state -> DamageType.entries.filter { stateOf(it) == state }.map { it to state } }

/** "FIRE:IMMUNITY,COLD:RESISTANCE" - what a column stores */
fun Map<DamageType, DamageState>.encodeDamage(): String =
    DamageType.entries
        .filter { stateOf(it) != DamageState.UNKNOWN }
        .joinToString(",") { "${it.name}:${stateOf(it).name}" }

/** reads what [encodeDamage] wrote, a pair this build does not know is skipped instead of failing the row */
fun decodeDamage(raw: String): Map<DamageType, DamageState> =
    raw
        .split(',')
        .mapNotNull { pair ->
            val (type, state) = pair.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val damageType = DamageType.entries.firstOrNull { it.name == type } ?: return@mapNotNull null
            val damageState = DamageState.entries.firstOrNull { it.name == state } ?: return@mapNotNull null
            damageType to damageState
        }.filter { it.second != DamageState.UNKNOWN }
        .toMap()
