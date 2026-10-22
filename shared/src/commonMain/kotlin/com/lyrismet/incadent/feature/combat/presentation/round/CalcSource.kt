package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_overline_damage
import dndplayerscodex.shared.generated.resources.combat_calc_overline_round
import dndplayerscodex.shared.generated.resources.combat_calc_source_default
import dndplayerscodex.shared.generated.resources.combat_calc_source_env
import dndplayerscodex.shared.generated.resources.combat_calc_source_environment
import dndplayerscodex.shared.generated.resources.combat_calc_source_other
import dndplayerscodex.shared.generated.resources.combat_calc_source_turn
import dndplayerscodex.shared.generated.resources.combat_calc_title_damage
import dndplayerscodex.shared.generated.resources.combat_calc_title_heal
import dndplayerscodex.shared.generated.resources.combat_calc_title_temp
import org.jetbrains.compose.resources.stringResource

internal fun titleOf(mode: CalcMode) =
    when (mode) {
        CalcMode.DAMAGE -> Res.string.combat_calc_title_damage
        CalcMode.HEAL -> Res.string.combat_calc_title_heal
        CalcMode.TEMP -> Res.string.combat_calc_title_temp
    }

@Composable
internal fun sourceName(
    id: Long?,
    ctx: CalcContext,
): String =
    if (id == null) {
        stringResource(Res.string.combat_calc_source_environment)
    } else {
        ctx.ordered.firstOrNull { it.id == id }?.name ?: ctx.mover?.name.orEmpty()
    }

@Composable
internal fun overline(
    draft: CalcDraft,
    ctx: CalcContext,
    round: Int,
    source: String,
): String =
    if (draft.mode == CalcMode.DAMAGE) {
        stringResource(Res.string.combat_calc_overline_damage, source.uppercase(), draft.hit)
    } else {
        stringResource(
            Res.string.combat_calc_overline_round,
            round,
            ctx.mover
                ?.name
                .orEmpty()
                .uppercase(),
        )
    }

@Composable
internal fun sourceChips(
    draft: CalcDraft,
    ctx: CalcContext,
): List<SourceChip> {
    val members =
        ctx.ordered.map { member ->
            val label =
                if (member.id ==
                    ctx.mover?.id
                ) {
                    stringResource(Res.string.combat_calc_source_turn, member.name)
                } else {
                    member.name
                }
            SourceChip(member.id, label, member.rowSide(), selected = draft.sourceId == member.id)
        }
    val env =
        SourceChip(
            null,
            stringResource(Res.string.combat_calc_source_environment),
            RowSide.NEUTRAL,
            draft.sourceId == null,
        )
    return members + env
}

@Composable
internal fun sourceHint(
    draft: CalcDraft,
    ctx: CalcContext,
): String =
    stringResource(
        when {
            draft.sourceId == null -> Res.string.combat_calc_source_env
            draft.sourceId == ctx.mover?.id -> Res.string.combat_calc_source_default
            else -> Res.string.combat_calc_source_other
        },
    )
