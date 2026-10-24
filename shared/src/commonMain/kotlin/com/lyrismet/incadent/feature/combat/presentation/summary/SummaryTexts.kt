package com.lyrismet.incadent.feature.combat.presentation.summary

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.results.LogEntry
import com.lyrismet.incadent.feature.combat.domain.results.ParticipantResult
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_log_damage
import dndplayerscodex.shared.generated.resources.combat_log_damage_alone
import dndplayerscodex.shared.generated.resources.combat_log_dead
import dndplayerscodex.shared.generated.resources.combat_log_down
import dndplayerscodex.shared.generated.resources.combat_log_down_by
import dndplayerscodex.shared.generated.resources.combat_log_down_party
import dndplayerscodex.shared.generated.resources.combat_log_heal
import dndplayerscodex.shared.generated.resources.combat_log_heal_alone
import dndplayerscodex.shared.generated.resources.combat_log_mark
import dndplayerscodex.shared.generated.resources.combat_log_stance_enemy
import dndplayerscodex.shared.generated.resources.combat_log_stance_fled
import dndplayerscodex.shared.generated.resources.combat_log_stance_friend
import dndplayerscodex.shared.generated.resources.combat_log_stance_neutral
import dndplayerscodex.shared.generated.resources.combat_log_temp
import dndplayerscodex.shared.generated.resources.combat_log_up
import dndplayerscodex.shared.generated.resources.combat_result_down_by
import dndplayerscodex.shared.generated.resources.combat_result_fled
import dndplayerscodex.shared.generated.resources.combat_result_hp
import dndplayerscodex.shared.generated.resources.combat_result_joined
import dndplayerscodex.shared.generated.resources.combat_result_surrendered
import dndplayerscodex.shared.generated.resources.combat_wound_barely
import dndplayerscodex.shared.generated.resources.combat_wound_bloodied
import dndplayerscodex.shared.generated.resources.combat_wound_captured
import dndplayerscodex.shared.generated.resources.combat_wound_dead
import dndplayerscodex.shared.generated.resources.combat_wound_down
import dndplayerscodex.shared.generated.resources.combat_wound_dying
import dndplayerscodex.shared.generated.resources.combat_wound_whole
import dndplayerscodex.shared.generated.resources.combat_wound_wounded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun CombatState.word(): StringResource =
    when (this) {
        CombatState.WHOLE -> Res.string.combat_wound_whole
        CombatState.WOUNDED -> Res.string.combat_wound_wounded
        CombatState.BLOODIED -> Res.string.combat_wound_bloodied
        CombatState.BARELY_STANDING -> Res.string.combat_wound_barely
        CombatState.CAPTURED -> Res.string.combat_wound_captured
        CombatState.DYING -> Res.string.combat_wound_dying
        CombatState.DOWN -> Res.string.combat_wound_down
    }

@Composable
fun ParticipantResult.text(): String =
    when (this) {
        ParticipantResult.Dead -> stringResource(Res.string.combat_wound_dead)
        ParticipantResult.Dying -> stringResource(Res.string.combat_wound_dying)
        is ParticipantResult.Hp -> stringResource(Res.string.combat_result_hp, current, max)
        is ParticipantResult.Looks -> stringResource(state.word())
        ParticipantResult.Captured -> stringResource(Res.string.combat_wound_captured)
        ParticipantResult.Fled -> stringResource(Res.string.combat_result_fled)
        ParticipantResult.Surrendered -> stringResource(Res.string.combat_result_surrendered)
        ParticipantResult.Joined -> stringResource(Res.string.combat_result_joined)
        is ParticipantResult.Down ->
            if (by == null) {
                stringResource(Res.string.combat_wound_down)
            } else {
                stringResource(Res.string.combat_result_down_by, by)
            }
    }

/** red for the fallen, blue for those who gave up or came over, grey for those who ran */
fun ParticipantResult.color(): Color =
    when (this) {
        ParticipantResult.Dead, ParticipantResult.Dying, is ParticipantResult.Down -> AppPalette.MaroonBright
        ParticipantResult.Captured, ParticipantResult.Surrendered, ParticipantResult.Joined -> AppPalette.Azure
        ParticipantResult.Fled -> AppPalette.TextTertiary
        is ParticipantResult.Hp, is ParticipantResult.Looks -> AppPalette.TextSecondary
    }

/** the dot before a name tells the side */
fun CombatSide.dotColor(): Color =
    when (this) {
        CombatSide.PARTY -> AppPalette.GoldBright
        CombatSide.ALLY -> AppPalette.EmeraldBright
        CombatSide.ENEMY -> AppPalette.MaroonBright
    }

@Composable
fun LogEntry.text(): String =
    when (this) {
        is LogEntry.Damage -> blowText(actor, target, amount, negative = true)
        is LogEntry.Heal -> blowText(actor, target, amount, negative = false)
        is LogEntry.Temp -> stringResource(Res.string.combat_log_temp, target, amount)
        is LogEntry.Down -> downText(target, party, by)
        is LogEntry.Up -> stringResource(Res.string.combat_log_up, target)
        is LogEntry.Dead -> stringResource(Res.string.combat_log_dead, target)
        is LogEntry.Mark -> stringResource(Res.string.combat_log_mark, target, stringResource(state.word()))
        is LogEntry.Stance -> stringResource(stance.logWord(), target)
    }

@Composable
private fun blowText(
    actor: String?,
    target: String,
    amount: Int,
    negative: Boolean,
): String =
    when {
        actor == null && negative -> stringResource(Res.string.combat_log_damage_alone, target, amount)
        actor == null -> stringResource(Res.string.combat_log_heal_alone, target, amount)
        negative -> stringResource(Res.string.combat_log_damage, actor, target, amount)
        else -> stringResource(Res.string.combat_log_heal, actor, target, amount)
    }

@Composable
private fun downText(
    target: String,
    party: Boolean,
    by: String?,
): String =
    when {
        party -> stringResource(Res.string.combat_log_down_party, target)
        by != null -> stringResource(Res.string.combat_log_down_by, target, by)
        else -> stringResource(Res.string.combat_log_down, target)
    }

private fun CombatStance.logWord(): StringResource =
    when (this) {
        CombatStance.ENEMY -> Res.string.combat_log_stance_enemy
        CombatStance.NEUTRAL -> Res.string.combat_log_stance_neutral
        CombatStance.FRIEND -> Res.string.combat_log_stance_friend
        CombatStance.FLED -> Res.string.combat_log_stance_fled
    }
