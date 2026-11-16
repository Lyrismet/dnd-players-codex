package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.damage.takeHeal
import com.lyrismet.incadent.feature.combat.domain.damage.takeTemp
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_calc_apply_damage
import incadent.shared.generated.resources.combat_calc_apply_enter
import incadent.shared.generated.resources.combat_calc_apply_heal
import incadent.shared.generated.resources.combat_calc_apply_pick
import incadent.shared.generated.resources.combat_calc_apply_targets
import incadent.shared.generated.resources.combat_calc_apply_temp
import incadent.shared.generated.resources.combat_calc_preview_heal_guess
import incadent.shared.generated.resources.combat_calc_preview_hp
import incadent.shared.generated.resources.combat_calc_preview_more
import incadent.shared.generated.resources.combat_calc_preview_targets
import incadent.shared.generated.resources.combat_calc_preview_temp
import incadent.shared.generated.resources.combat_calc_preview_temp_same
import incadent.shared.generated.resources.combat_calc_preview_untracked
import incadent.shared.generated.resources.hp_exact_apply
import incadent.shared.generated.resources.hp_exact_apply_heal
import incadent.shared.generated.resources.hp_exact_now
import incadent.shared.generated.resources.hp_exact_now_unknown
import org.jetbrains.compose.resources.stringResource

private const val PREVIEW_LINES = 3

@Composable
internal fun applyLabel(
    draft: CalcDraft,
    targetCount: Int,
    sum: Int,
    heal: Int = 0,
): String {
    if (draft.mode == CalcMode.SET) return setLabel(draft, targetCount, sum, heal)
    val base =
        when {
            targetCount == 0 -> stringResource(Res.string.combat_calc_apply_pick)
            draft.mode == CalcMode.DAMAGE -> stringResource(Res.string.combat_calc_apply_damage)
            sum == 0 -> stringResource(Res.string.combat_calc_apply_enter)
            draft.mode == CalcMode.HEAL -> stringResource(Res.string.combat_calc_apply_heal, sum)
            else -> stringResource(Res.string.combat_calc_apply_temp, sum)
        }
    val counted = targetCount > 1 && sum > 0 && draft.mode != CalcMode.DAMAGE
    return if (counted) stringResource(Res.string.combat_calc_apply_targets, base, targetCount) else base
}

@Composable
private fun setLabel(
    draft: CalcDraft,
    targetCount: Int,
    sum: Int,
    heal: Int,
): String =
    when {
        targetCount != 1 -> stringResource(Res.string.combat_calc_apply_pick)
        draft.expr.raw.isEmpty() -> stringResource(Res.string.combat_calc_apply_enter)
        heal > 0 -> stringResource(Res.string.hp_exact_apply_heal, sum, heal)
        else -> stringResource(Res.string.hp_exact_apply, sum)
    }

@Composable
internal fun previewLines(
    draft: CalcDraft,
    targets: List<Combatant>,
    sum: Int,
): List<String> {
    if (sum == 0) {
        val names = targets.joinToString(", ") { it.name }
        return listOf(
            if (targets.isEmpty()) {
                stringResource(
                    Res.string.combat_calc_apply_pick,
                )
            } else {
                stringResource(Res.string.combat_calc_preview_targets, names)
            },
        )
    }
    val lines = targets.take(PREVIEW_LINES).map { previewLine(draft.mode, it, sum) }
    val more = targets.size - PREVIEW_LINES
    return if (more > 0) lines + stringResource(Res.string.combat_calc_preview_more, more) else lines
}

@Composable
internal fun previewLine(
    mode: CalcMode,
    member: Combatant,
    sum: Int,
): String =
    if (mode == CalcMode.SET) {
        val tracked = member.isTracked()
        stringResource(
            if (tracked) Res.string.hp_exact_now else Res.string.hp_exact_now_unknown,
            member.name,
            if (tracked) sum.coerceAtMost(checkNotNull(member.hpMax)) else sum,
        )
    } else {
        countedPreview(mode, member, sum)
    }

@Composable
private fun countedPreview(
    mode: CalcMode,
    member: Combatant,
    sum: Int,
): String {
    if (!member.isTracked()) {
        return if (mode == CalcMode.HEAL) {
            stringResource(Res.string.combat_calc_preview_heal_guess, member.name, sum)
        } else {
            stringResource(Res.string.combat_calc_preview_untracked, member.name)
        }
    }
    val max = checkNotNull(member.hpMax)
    val now = member.hpCurrent ?: max
    return if (mode == CalcMode.HEAL) {
        stringResource(Res.string.combat_calc_preview_hp, member.name, now, takeHeal(now, max, member.tempHp, sum).hp)
    } else {
        val change = takeTemp(now, member.tempHp, sum)
        val key =
            if (sum <= member.tempHp &&
                member.tempHp > 0
            ) {
                Res.string.combat_calc_preview_temp_same
            } else {
                Res.string.combat_calc_preview_temp
            }
        stringResource(key, member.name, member.tempHp, change.temp)
    }
}
