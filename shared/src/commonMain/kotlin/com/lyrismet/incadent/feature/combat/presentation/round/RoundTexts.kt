package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDying
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_wound_barely
import dndplayerscodex.shared.generated.resources.combat_wound_bloodied
import dndplayerscodex.shared.generated.resources.combat_wound_dead
import dndplayerscodex.shared.generated.resources.combat_wound_down
import dndplayerscodex.shared.generated.resources.combat_wound_down_mark
import dndplayerscodex.shared.generated.resources.combat_wound_dying
import dndplayerscodex.shared.generated.resources.combat_wound_say_barely
import dndplayerscodex.shared.generated.resources.combat_wound_say_bloodied
import dndplayerscodex.shared.generated.resources.combat_wound_say_down
import dndplayerscodex.shared.generated.resources.combat_wound_say_whole
import dndplayerscodex.shared.generated.resources.combat_wound_say_wounded
import dndplayerscodex.shared.generated.resources.combat_wound_whole
import dndplayerscodex.shared.generated.resources.combat_wound_wounded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** the five steps of the scale, from unhurt to down */
internal val WoundScale =
    listOf(CombatState.WHOLE, CombatState.WOUNDED, CombatState.BLOODIED, CombatState.BARELY_STANDING, CombatState.DOWN)

internal fun CombatState.tone(): WoundTone =
    when (this) {
        CombatState.WHOLE -> WoundTone.WHOLE
        CombatState.WOUNDED -> WoundTone.WOUNDED
        CombatState.BLOODIED -> WoundTone.BLOODIED
        CombatState.BARELY_STANDING -> WoundTone.BARELY
        else -> WoundTone.DOWN
    }

/** how many of the five bars a state fills */
internal fun CombatState.pips(): Int {
    val index = WoundScale.indexOf(this)
    return if (index < 0) 1 else WoundScale.size - index
}

internal fun CombatState.shortLabel(): StringResource =
    when (this) {
        CombatState.WHOLE -> Res.string.combat_wound_whole
        CombatState.WOUNDED -> Res.string.combat_wound_wounded
        CombatState.BLOODIED -> Res.string.combat_wound_bloodied
        CombatState.BARELY_STANDING -> Res.string.combat_wound_barely
        else -> Res.string.combat_wound_down_mark
    }

internal fun CombatState.longLabel(): StringResource =
    when (this) {
        CombatState.WHOLE -> Res.string.combat_wound_whole
        CombatState.WOUNDED -> Res.string.combat_wound_wounded
        CombatState.BLOODIED -> Res.string.combat_wound_bloodied
        CombatState.BARELY_STANDING -> Res.string.combat_wound_barely
        else -> Res.string.combat_wound_down
    }

internal fun CombatState.say(): StringResource =
    when (this) {
        CombatState.WHOLE -> Res.string.combat_wound_say_whole
        CombatState.WOUNDED -> Res.string.combat_wound_say_wounded
        CombatState.BLOODIED -> Res.string.combat_wound_say_bloodied
        CombatState.BARELY_STANDING -> Res.string.combat_wound_say_barely
        else -> Res.string.combat_wound_say_down
    }

/** the word and the colour a row shows for the participant's state, a dying or a dead one has its own */
@Composable
internal fun woundWord(member: Combatant): Pair<String, WoundTone> =
    when {
        member.dead -> stringResource(Res.string.combat_wound_dead) to WoundTone.DEAD
        member.isDying() -> stringResource(Res.string.combat_wound_dying) to WoundTone.DYING
        else -> stringResource(member.state.shortLabel()) to member.state.tone()
    }

internal fun Combatant.rowSide(): RowSide =
    when {
        side == CombatSide.PARTY -> RowSide.PARTY
        side == CombatSide.ALLY -> RowSide.ALLY
        else ->
            when (stance) {
                CombatStance.ENEMY -> RowSide.ENEMY
                CombatStance.NEUTRAL -> RowSide.NEUTRAL
                CombatStance.FRIEND -> RowSide.FRIEND
                CombatStance.FLED -> RowSide.FLED
            }
    }
