package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isDying
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_panel_hp_sub
import dndplayerscodex.shared.generated.resources.combat_panel_hp_sub_temp
import dndplayerscodex.shared.generated.resources.combat_panel_note_foe
import dndplayerscodex.shared.generated.resources.combat_panel_note_party
import dndplayerscodex.shared.generated.resources.combat_panel_side_ally
import dndplayerscodex.shared.generated.resources.combat_panel_side_enemy
import dndplayerscodex.shared.generated.resources.combat_panel_side_fled
import dndplayerscodex.shared.generated.resources.combat_panel_side_friend
import dndplayerscodex.shared.generated.resources.combat_panel_side_neutral
import dndplayerscodex.shared.generated.resources.combat_panel_side_party
import dndplayerscodex.shared.generated.resources.combat_panel_source
import dndplayerscodex.shared.generated.resources.combat_panel_source_note
import dndplayerscodex.shared.generated.resources.combat_panel_stance_npc
import dndplayerscodex.shared.generated.resources.combat_panel_stance_plain
import dndplayerscodex.shared.generated.resources.combat_round_no_damage
import dndplayerscodex.shared.generated.resources.combat_wound_say_line
import dndplayerscodex.shared.generated.resources.combat_wound_since
import org.jetbrains.compose.resources.stringResource

/** the bottom panel of the chosen participant: how they look, what can be done to them, what they carry */
@Composable
internal fun roundPanel(
    combat: Combat,
    member: Combatant,
    role: String,
    moverName: String,
): RoundPanel {
    val tracked = member.isTracked()
    val dying = member.isDying()
    val down = member.isDown()
    val party = member.side == CombatSide.PARTY
    return RoundPanel(
        id = member.id,
        name = member.name,
        sideLabel = sideLabel(member, role),
        side = member.rowSide(),
        hp = if (tracked) panelHp(member) else null,
        wound = if (tracked) null else panelWound(combat, member),
        marks = if (!tracked && !member.dead) markOptions(member) else null,
        sayLine = if (!tracked) sayLine(member) else "",
        saves = if (dying) SavesState(member.deathSuccesses, member.deathFailures) else null,
        down =
            if (down && !member.dead) {
                DownState(
                    canStandUp = tracked,
                    canCapture = !party,
                    captured = member.captured,
                    canDie = dying,
                )
            } else {
                null
            },
        died = member.dead,
        conditions = conditionGroups(member),
        more = panelMore(member, tracked, party),
        calcModes = if (tracked) CalcMode.entries.toList() else listOf(CalcMode.DAMAGE, CalcMode.HEAL),
        sourceNote = stringResource(Res.string.combat_panel_source_note, moverName),
    )
}

@Composable
private fun sideLabel(
    member: Combatant,
    role: String,
): String {
    val label =
        when {
            member.side == CombatSide.PARTY -> Res.string.combat_panel_side_party
            member.side == CombatSide.ALLY -> Res.string.combat_panel_side_ally
            member.stance == CombatStance.NEUTRAL -> Res.string.combat_panel_side_neutral
            member.stance == CombatStance.FRIEND -> Res.string.combat_panel_side_friend
            member.stance == CombatStance.FLED -> Res.string.combat_panel_side_fled
            else -> Res.string.combat_panel_side_enemy
        }
    return stringResource(label, role.uppercase())
}

@Composable
private fun panelHp(member: Combatant): PanelHp {
    val max = checkNotNull(member.hpMax)
    val caption =
        if (member.tempHp > 0) {
            stringResource(Res.string.combat_panel_hp_sub_temp, member.tempHp)
        } else {
            stringResource(Res.string.combat_panel_hp_sub)
        }
    return PanelHp((member.hpCurrent ?: max).toString(), max.toString(), caption)
}

@Composable
private fun panelWound(
    combat: Combat,
    member: Combatant,
): PanelWound {
    val word = woundWord(member)
    val mark = combat.markAt(member)
    val label =
        if (mark != null &&
            !member.isDown()
        ) {
            stringResource(Res.string.combat_wound_since, word.first, mark)
        } else {
            word.first
        }
    val taken = combat.damageTaken(member)
    val forecast = combat.forecast(member)
    val sources =
        listOfNotNull(
            if (taken > 0) tookText(taken) else stringResource(Res.string.combat_round_no_damage),
            forecast.takeIf { it != HitForecast.Unknown }?.longText(),
        )
    val source =
        if (sources.size ==
            2
        ) {
            stringResource(Res.string.combat_panel_source, sources[0], sources[1])
        } else {
            sources[0]
        }
    return PanelWound(label, word.second, source)
}

@Composable
private fun markOptions(member: Combatant): List<MarkOption> =
    WoundScale.reversed().map { state ->
        val selected = member.state == state || (state == CombatState.DOWN && member.isDown())
        MarkOption(state, stringResource(state.shortLabel()), state.tone(), selected)
    }

@Composable
private fun sayLine(member: Combatant): String {
    val word = woundWord(member).first
    return stringResource(Res.string.combat_wound_say_line, word, stringResource(member.state.say()))
}

@Composable
private fun conditionGroups(member: Combatant): List<ConditionGroupItem> =
    ConditionGroup.entries.map { group ->
        ConditionGroupItem(
            group = group,
            title = group.title(),
            items =
                CombatCondition.entries
                    .filter { it.group == group }
                    .map { ConditionItem(it, it.label(), it in member.conditions) },
        )
    }

@Composable
private fun panelMore(
    member: Combatant,
    tracked: Boolean,
    party: Boolean,
): PanelMore {
    val enemy = member.side == CombatSide.ENEMY
    return PanelMore(
        stances = if (enemy) CombatStance.entries.map { StanceOption(it, it.label(), it == member.stance) } else null,
        stanceHint =
            stringResource(
                if (member.npcId !=
                    null
                ) {
                    Res.string.combat_panel_stance_npc
                } else {
                    Res.string.combat_panel_stance_plain
                },
            ),
        fullShown = !tracked && !party,
        full = member.enteredWhole,
        canSetHp = !tracked && !member.dead,
        note = member.note,
        notePlaceholder =
            stringResource(
                if (party) Res.string.combat_panel_note_party else Res.string.combat_panel_note_foe,
            ),
    )
}
