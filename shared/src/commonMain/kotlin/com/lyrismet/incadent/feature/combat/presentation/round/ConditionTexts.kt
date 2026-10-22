package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_condition_blinded
import dndplayerscodex.shared.generated.resources.combat_condition_charmed
import dndplayerscodex.shared.generated.resources.combat_condition_concentration
import dndplayerscodex.shared.generated.resources.combat_condition_deafened
import dndplayerscodex.shared.generated.resources.combat_condition_frightened
import dndplayerscodex.shared.generated.resources.combat_condition_grappled
import dndplayerscodex.shared.generated.resources.combat_condition_group_effect
import dndplayerscodex.shared.generated.resources.combat_condition_group_restraint
import dndplayerscodex.shared.generated.resources.combat_condition_group_weakening
import dndplayerscodex.shared.generated.resources.combat_condition_incapacitated
import dndplayerscodex.shared.generated.resources.combat_condition_invisible
import dndplayerscodex.shared.generated.resources.combat_condition_paralyzed
import dndplayerscodex.shared.generated.resources.combat_condition_petrified
import dndplayerscodex.shared.generated.resources.combat_condition_poisoned
import dndplayerscodex.shared.generated.resources.combat_condition_prone
import dndplayerscodex.shared.generated.resources.combat_condition_restrained
import dndplayerscodex.shared.generated.resources.combat_condition_stunned
import dndplayerscodex.shared.generated.resources.combat_stance_enemy
import dndplayerscodex.shared.generated.resources.combat_stance_fled
import dndplayerscodex.shared.generated.resources.combat_stance_friend
import dndplayerscodex.shared.generated.resources.combat_stance_neutral
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CombatStance.label(): String =
    stringResource(
        when (this) {
            CombatStance.ENEMY -> Res.string.combat_stance_enemy
            CombatStance.NEUTRAL -> Res.string.combat_stance_neutral
            CombatStance.FRIEND -> Res.string.combat_stance_friend
            CombatStance.FLED -> Res.string.combat_stance_fled
        },
    )

internal fun ConditionGroup.chipKind(): ChipKind =
    when (this) {
        ConditionGroup.RESTRAINT -> ChipKind.RESTRAINT
        ConditionGroup.WEAKENING -> ChipKind.WEAKENING
        ConditionGroup.EFFECT -> ChipKind.EFFECT
    }

@Composable
internal fun ConditionGroup.title(): String =
    stringResource(
        when (this) {
            ConditionGroup.RESTRAINT -> Res.string.combat_condition_group_restraint
            ConditionGroup.WEAKENING -> Res.string.combat_condition_group_weakening
            ConditionGroup.EFFECT -> Res.string.combat_condition_group_effect
        },
    )

private val ConditionLabels: Map<CombatCondition, StringResource> =
    mapOf(
        CombatCondition.GRAPPLED to Res.string.combat_condition_grappled,
        CombatCondition.RESTRAINED to Res.string.combat_condition_restrained,
        CombatCondition.PRONE to Res.string.combat_condition_prone,
        CombatCondition.STUNNED to Res.string.combat_condition_stunned,
        CombatCondition.PARALYZED to Res.string.combat_condition_paralyzed,
        CombatCondition.INCAPACITATED to Res.string.combat_condition_incapacitated,
        CombatCondition.PETRIFIED to Res.string.combat_condition_petrified,
        CombatCondition.BLINDED to Res.string.combat_condition_blinded,
        CombatCondition.DEAFENED to Res.string.combat_condition_deafened,
        CombatCondition.POISONED to Res.string.combat_condition_poisoned,
        CombatCondition.FRIGHTENED to Res.string.combat_condition_frightened,
        CombatCondition.CHARMED to Res.string.combat_condition_charmed,
        CombatCondition.CONCENTRATION to Res.string.combat_condition_concentration,
        CombatCondition.INVISIBLE to Res.string.combat_condition_invisible,
    )

@Composable
internal fun CombatCondition.label(): String = stringResource(ConditionLabels.getValue(this))
