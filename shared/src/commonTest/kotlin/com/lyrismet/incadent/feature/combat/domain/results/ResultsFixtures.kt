package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlinx.datetime.LocalDateTime

internal fun member(
    id: Long,
    name: String,
    side: CombatSide,
    speciesId: Long? = null,
    npcId: Long? = null,
    state: CombatState = CombatState.WHOLE,
    enteredWhole: Boolean = true,
) = Combatant(
    id = id,
    name = name,
    side = side,
    npcId = npcId,
    speciesId = speciesId,
    partyMemberId = null,
    enteredWhole = enteredWhole,
    state = state,
    joinedRound = 1,
    deathSuccesses = 0,
    deathFailures = 0,
)

internal fun strike(
    id: Long,
    source: Long?,
    target: Long,
    amount: Int,
    type: DamageType? = DamageType.SLASHING,
    effect: HitEffect = HitEffect.FULL,
    hit: Int = id.toInt(),
) = CombatEvent(id, 1, CombatEventKind.DAMAGE, source, target, amount, type, effect, null, hit = hit)

internal fun marker(
    id: Long,
    kind: CombatEventKind,
    source: Long?,
    target: Long,
) = CombatEvent(id, 1, kind, source, target, 0, null, HitEffect.FULL, null)

internal fun combatOf(
    members: List<Combatant>,
    events: List<CombatEvent> = emptyList(),
) = Combat(
    id = 1,
    sessionNoteId = null,
    phase = CombatPhase.ONGOING,
    startedAt = LocalDateTime(2026, 1, 1, 12, 0),
    round = 1,
    turn = 0,
    outcome = null,
    combatants = members,
    events = events,
)

internal fun speciesOf(
    id: Long,
    name: String = "Гоблин",
    damage: Map<DamageType, DamageState> = emptyMap(),
) = Species(id, name, CreatureType.HUMANOID, damage)
