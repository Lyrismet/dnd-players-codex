package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_condition_asleep
import incadent.shared.generated.resources.combat_condition_banished
import incadent.shared.generated.resources.combat_condition_blinded
import incadent.shared.generated.resources.combat_condition_charmed
import incadent.shared.generated.resources.combat_condition_concentration
import incadent.shared.generated.resources.combat_condition_deafened
import incadent.shared.generated.resources.combat_condition_frightened
import incadent.shared.generated.resources.combat_condition_grappled
import incadent.shared.generated.resources.combat_condition_group_effect
import incadent.shared.generated.resources.combat_condition_group_restraint
import incadent.shared.generated.resources.combat_condition_group_weakening
import incadent.shared.generated.resources.combat_condition_hasted
import incadent.shared.generated.resources.combat_condition_incapacitated
import incadent.shared.generated.resources.combat_condition_invisible
import incadent.shared.generated.resources.combat_condition_paralyzed
import incadent.shared.generated.resources.combat_condition_petrified
import incadent.shared.generated.resources.combat_condition_poisoned
import incadent.shared.generated.resources.combat_condition_prone
import incadent.shared.generated.resources.combat_condition_restrained
import incadent.shared.generated.resources.combat_condition_silenced
import incadent.shared.generated.resources.combat_condition_slowed
import incadent.shared.generated.resources.combat_condition_stunned
import incadent.shared.generated.resources.combat_stance_enemy
import incadent.shared.generated.resources.combat_stance_fled
import incadent.shared.generated.resources.combat_stance_friend
import incadent.shared.generated.resources.combat_stance_neutral
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
        CombatCondition.ASLEEP to Res.string.combat_condition_asleep,
        CombatCondition.BANISHED to Res.string.combat_condition_banished,
        CombatCondition.SILENCED to Res.string.combat_condition_silenced,
        CombatCondition.SLOWED to Res.string.combat_condition_slowed,
        CombatCondition.HASTED to Res.string.combat_condition_hasted,
    )

@Composable
internal fun CombatCondition.label(): String = stringResource(ConditionLabels.getValue(this))
