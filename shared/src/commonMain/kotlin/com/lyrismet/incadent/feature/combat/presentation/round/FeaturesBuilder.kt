package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** the facts a participant has: its record's when it has one, else the ones the fight keeps */
internal fun factsOf(
    member: Combatant,
    npc: Npc?,
): List<String> = npc?.facts ?: member.facts

internal fun featuresBlock(
    member: Combatant,
    npc: Npc?,
    effective: Map<DamageType, DamageState>,
): FeaturesBlock {
    val shields = effective.shields()
    val facts = factsOf(member, npc)
    return FeaturesBlock(
        shields = shields.take(FEATURE_SHIELDS_SHOWN).map { ShieldItem(it.first, it.second) },
        facts = facts.take(FEATURE_FACTS_SHOWN),
        onlyInFight = npc == null,
        cells = DamageType.entries.map { ShieldCell(it, effective[it]?.takeIf { s -> s != DamageState.NORMAL }) },
        hidden =
            (shields.size - FEATURE_SHIELDS_SHOWN).coerceAtLeast(0) +
                (facts.size - FEATURE_FACTS_SHOWN).coerceAtLeast(0),
    )
}

internal fun featuresSheet(
    draft: FeaturesDraft,
    member: Combatant,
    npc: Npc?,
    effective: Map<DamageType, DamageState>,
): FeaturesSheet =
    FeaturesSheet(
        name = member.name,
        onlyInFight = npc == null,
        facts = factsOf(member, npc),
        text = draft.text,
        canAdd = draft.text.isNotBlank(),
        shields = effective,
    )

internal fun featuresSheetFor(
    draft: FeaturesDraft?,
    combat: Combat,
    npcs: List<Npc>,
    resistances: Map<Long, Map<DamageType, DamageState>>,
): FeaturesSheet? =
    draft?.let { d ->
        combat.combatants.firstOrNull { it.id == d.memberId }?.let { member ->
            featuresSheet(d, member, npcs.firstOrNull { it.id == member.npcId }, resistances[member.id].orEmpty())
        }
    }
