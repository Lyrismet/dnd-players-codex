package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isDying
import com.lyrismet.incadent.feature.combat.domain.round.isOut
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_panel_rx_attacked
import dndplayerscodex.shared.generated.resources.combat_panel_rx_available
import dndplayerscodex.shared.generated.resources.combat_panel_rx_spent
import dndplayerscodex.shared.generated.resources.combat_round_reinforcement
import dndplayerscodex.shared.generated.resources.combat_round_you
import dndplayerscodex.shared.generated.resources.combat_wound_captured
import org.jetbrains.compose.resources.stringResource

private const val FIVE_PIPS = 5
private const val HIGH_RATIO = 0.5f
private const val MID_RATIO = 0.25f

/** what the master calls a row: the class of a party member, or what a creature is, a reinforcement says so */
@Composable
internal fun roleOf(
    member: Combatant,
    party: List<PartyMember>,
    kind: String,
): String {
    val own = party.firstOrNull { it.id == member.partyMemberId }
    val base = own?.characterClass ?: kind
    val named = if (own?.isPlayerCharacter == true) stringResource(Res.string.combat_round_you, base) else base
    return if (member.joinedRound > 1) stringResource(Res.string.combat_round_reinforcement, named) else named
}

@Composable
internal fun roundRows(
    combat: Combat,
    ordered: List<Combatant>,
    currentId: Long?,
    selectedId: Long?,
    party: List<PartyMember>,
    kinds: Map<Long, String>,
): List<RoundRow> {
    val current = ordered.firstOrNull { it.id == currentId }
    return ordered.map { member ->
        val own = party.any { it.id == member.partyMemberId && it.isPlayerCharacter }
        roundRow(
            combat,
            member,
            member.id == currentId,
            member.id == selectedId,
            own,
            current,
            party,
            kinds.getValue(member.id),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun roundRow(
    combat: Combat,
    member: Combatant,
    isCurrent: Boolean,
    isSelected: Boolean,
    own: Boolean,
    current: Combatant?,
    party: List<PartyMember>,
    kind: String,
): RoundRow {
    val down = member.isDown()
    val tracked = member.isTracked()
    val forecast = combat.forecast(member)
    val taken = combat.damageTaken(member)
    val word = woundWord(member)
    val caption =
        when {
            tracked -> ""
            forecast != HitForecast.Unknown -> forecast.shortText().orEmpty()
            taken > 0 -> tookText(taken)
            else -> ""
        }
    return RoundRow(
        id = member.id,
        badge = member.turnOrder?.toString() ?: "—",
        current = isCurrent,
        selected = isSelected,
        name = member.name,
        struck = member.dead,
        role = roleOf(member, party, kind),
        side = member.rowSide(),
        lastHit = null,
        hp = if (tracked) rowHp(member) else null,
        wound = if (tracked && !member.isDying()) null else rowWound(member, word),
        caption = caption,
        chips = rowChips(member),
        dimmed = (down && !member.isDying()) || member.stance == CombatStance.FLED,
        reaction = if (own && !member.isOut()) rowReaction(member, current) else null,
    )
}

private fun rowHp(member: Combatant): RowHp {
    val max = checkNotNull(member.hpMax)
    val now = member.hpCurrent ?: max
    val ratio = if (max > 0) now.toFloat() / max else 0f
    val level =
        when {
            now == 0 -> HpLevel.ZERO
            ratio > HIGH_RATIO -> HpLevel.HIGH
            ratio > MID_RATIO -> HpLevel.MID
            else -> HpLevel.LOW
        }
    return RowHp(now.toString(), max.toString(), member.tempHp, ratio.coerceIn(0f, 1f), level)
}

private fun rowWound(
    member: Combatant,
    word: Pair<String, WoundTone>,
): RowWound {
    val dying = member.isDying()
    return RowWound(
        label = word.first,
        tone = word.second,
        pips = if (dying) 0 else member.state.pips().coerceAtMost(FIVE_PIPS),
        successes = if (dying) member.deathSuccesses else null,
        failures = if (dying) member.deathFailures else null,
    )
}

@Composable
private fun rowChips(member: Combatant): List<RowChip> {
    val chips = mutableListOf<RowChip>()
    if (member.captured) chips += RowChip(stringResource(Res.string.combat_wound_captured), ChipKind.CAPTURED)
    if (member.side == CombatSide.ENEMY && member.stance != CombatStance.ENEMY) {
        val kind =
            when (member.stance) {
                CombatStance.NEUTRAL -> ChipKind.NEUTRAL
                CombatStance.FRIEND -> ChipKind.FRIEND
                else -> ChipKind.FLED
            }
        chips += RowChip(member.stance.label(), kind)
    }
    member.conditions.forEach { chips += RowChip(it.label(), it.group.chipKind()) }
    return chips
}

@Composable
private fun rowReaction(
    member: Combatant,
    current: Combatant?,
): RowReaction {
    val label =
        stringResource(
            if (member.reactionAvailable) Res.string.combat_panel_rx_available else Res.string.combat_panel_rx_spent,
        )
    val pulsing = member.reactionAvailable && current?.side == CombatSide.ENEMY
    return RowReaction(
        available = member.reactionAvailable,
        pulsing = pulsing,
        label = if (pulsing) stringResource(Res.string.combat_panel_rx_attacked) else label,
    )
}
