package com.lyrismet.incadent.feature.combat.data

import com.lyrismet.incadent.feature.codex.domain.grouping.decodeFacts
import com.lyrismet.incadent.feature.codex.domain.grouping.decodeVulnerabilities
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatMark
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.CustomCondition
import com.lyrismet.incadent.db.Combat as CombatRow
import com.lyrismet.incadent.db.Combat_event as CombatEventRow
import com.lyrismet.incadent.db.Combatant as CombatantRow
import com.lyrismet.incadent.db.Combatant_mark as MarkRow
import com.lyrismet.incadent.db.Custom_condition as CustomConditionRow

internal fun CombatRow.toDomain(
    combatants: List<Combatant>,
    events: List<CombatEvent>,
): Combat =
    Combat(
        id = id,
        sessionNoteId = session_note_id,
        phase = phase,
        startedAt = started_at,
        round = round.toInt(),
        turn = turn.toInt(),
        outcome = outcome,
        combatants = combatants,
        events = events,
        finishedAt = finished_at,
        originId = origin_id,
    )

/** [codexName] is the name of the record the participant is linked to, it wins over the copy kept here */
internal fun CombatantRow.toDomain(
    codexName: String? = null,
    marks: List<CombatMark> = emptyList(),
): Combatant =
    Combatant(
        id = id,
        name = codexName ?: name,
        side = side,
        npcId = npc_id,
        speciesId = species_id,
        partyMemberId = party_member_id,
        enteredWhole = entered_whole != 0L,
        state = state,
        joinedRound = joined_round.toInt(),
        deathSuccesses = death_successes.toInt(),
        deathFailures = death_failures.toInt(),
        tempHp = temp_hp.toInt(),
        turnOrder = turn_order?.toInt(),
        stance = stance,
        captured = captured != 0L,
        dead = dead != 0L,
        conditions = conditions,
        note = note,
        reactionAvailable = reaction_available != 0L,
        hpCurrent = hp_current?.toInt(),
        hpMax = hp_max?.toInt(),
        role = role,
        isLabel = is_label != 0L,
        marks = marks,
        vulnerabilities = decodeVulnerabilities(vulnerabilities),
        facts = decodeFacts(facts),
    )

internal fun CombatEventRow.toDomain(): CombatEvent =
    CombatEvent(
        id = id,
        round = round.toInt(),
        kind = kind,
        sourceId = source_id,
        targetId = target_id,
        amount = amount.toInt(),
        damageType = damage_type,
        effect = effect,
        state = state,
        detail = detail,
        turn = turn.toInt(),
        hit = hit.toInt(),
        raw = raw.toInt(),
        scale = scale,
    )

internal fun MarkRow.toDomain(): CombatMark =
    CombatMark(
        id = id,
        combatantId = combatant_id,
        kind = kind,
        text = text,
        roundsLeft = rounds_left?.toInt(),
        startedRound = started_round.toInt(),
    )

internal fun CustomConditionRow.toDomain(): CustomCondition = CustomCondition(id, name)
