package com.lyrismet.incadent.feature.combat.presentation.prep

import com.lyrismet.incadent.feature.combat.domain.grouping.baseName
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource

private const val NPC_PREFIX = "n:"
private const val SPECIES_PREFIX = "s:"
private const val TEMP_PREFIX = "t:"

/** the group a member belongs to in the preparation list: its npc, its species or its name for this fight */
internal fun Combatant.groupKey(): String =
    when {
        npcId != null -> NPC_PREFIX + npcId
        speciesId != null -> SPECIES_PREFIX + speciesId
        else -> TEMP_PREFIX + name.baseName()
    }

internal fun npcKey(id: Long): String = NPC_PREFIX + id

internal fun speciesKey(id: Long): String = SPECIES_PREFIX + id

/** the source a group key stands for, [nameOf] gives the record's name for an npc or a species id */
internal fun sourceOf(
    key: String,
    nameOf: (String) -> String?,
): MemberSource? =
    when {
        key.startsWith(NPC_PREFIX) ->
            key.removePrefix(NPC_PREFIX).toLongOrNull()?.let { id ->
                nameOf(key)?.let { MemberSource(npcId = id, speciesId = null, baseName = it) }
            }
        key.startsWith(SPECIES_PREFIX) ->
            key.removePrefix(SPECIES_PREFIX).toLongOrNull()?.let { id ->
                nameOf(key)?.let { MemberSource(npcId = null, speciesId = id, baseName = it) }
            }
        key.startsWith(TEMP_PREFIX) -> MemberSource(null, null, key.removePrefix(TEMP_PREFIX))
        else -> null
    }
