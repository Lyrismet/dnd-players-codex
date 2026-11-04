package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.sheet_ability_charisma
import dndplayerscodex.shared.generated.resources.sheet_ability_charisma_abbr
import dndplayerscodex.shared.generated.resources.sheet_ability_constitution
import dndplayerscodex.shared.generated.resources.sheet_ability_constitution_abbr
import dndplayerscodex.shared.generated.resources.sheet_ability_dexterity
import dndplayerscodex.shared.generated.resources.sheet_ability_dexterity_abbr
import dndplayerscodex.shared.generated.resources.sheet_ability_intelligence
import dndplayerscodex.shared.generated.resources.sheet_ability_intelligence_abbr
import dndplayerscodex.shared.generated.resources.sheet_ability_strength
import dndplayerscodex.shared.generated.resources.sheet_ability_strength_abbr
import dndplayerscodex.shared.generated.resources.sheet_ability_wisdom
import dndplayerscodex.shared.generated.resources.sheet_ability_wisdom_abbr
import dndplayerscodex.shared.generated.resources.sheet_skill_acrobatics
import dndplayerscodex.shared.generated.resources.sheet_skill_animal_handling
import dndplayerscodex.shared.generated.resources.sheet_skill_animal_handling_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_arcana
import dndplayerscodex.shared.generated.resources.sheet_skill_arcana_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_athletics
import dndplayerscodex.shared.generated.resources.sheet_skill_deception
import dndplayerscodex.shared.generated.resources.sheet_skill_history
import dndplayerscodex.shared.generated.resources.sheet_skill_insight
import dndplayerscodex.shared.generated.resources.sheet_skill_intimidation
import dndplayerscodex.shared.generated.resources.sheet_skill_investigation
import dndplayerscodex.shared.generated.resources.sheet_skill_investigation_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_medicine
import dndplayerscodex.shared.generated.resources.sheet_skill_nature
import dndplayerscodex.shared.generated.resources.sheet_skill_perception
import dndplayerscodex.shared.generated.resources.sheet_skill_perception_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_performance
import dndplayerscodex.shared.generated.resources.sheet_skill_performance_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_persuasion
import dndplayerscodex.shared.generated.resources.sheet_skill_religion
import dndplayerscodex.shared.generated.resources.sheet_skill_sleight_of_hand
import dndplayerscodex.shared.generated.resources.sheet_skill_sleight_of_hand_alt
import dndplayerscodex.shared.generated.resources.sheet_skill_stealth
import dndplayerscodex.shared.generated.resources.sheet_skill_survival
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// one flat line per skill, nothing to simplify
@Suppress("CyclomaticComplexMethod")
internal fun Skill.nameRes(): StringResource =
    when (this) {
        Skill.ACROBATICS -> Res.string.sheet_skill_acrobatics
        Skill.ANIMAL_HANDLING -> Res.string.sheet_skill_animal_handling
        Skill.ARCANA -> Res.string.sheet_skill_arcana
        Skill.ATHLETICS -> Res.string.sheet_skill_athletics
        Skill.DECEPTION -> Res.string.sheet_skill_deception
        Skill.HISTORY -> Res.string.sheet_skill_history
        Skill.INSIGHT -> Res.string.sheet_skill_insight
        Skill.INTIMIDATION -> Res.string.sheet_skill_intimidation
        Skill.INVESTIGATION -> Res.string.sheet_skill_investigation
        Skill.MEDICINE -> Res.string.sheet_skill_medicine
        Skill.NATURE -> Res.string.sheet_skill_nature
        Skill.PERCEPTION -> Res.string.sheet_skill_perception
        Skill.PERFORMANCE -> Res.string.sheet_skill_performance
        Skill.PERSUASION -> Res.string.sheet_skill_persuasion
        Skill.RELIGION -> Res.string.sheet_skill_religion
        Skill.SLEIGHT_OF_HAND -> Res.string.sheet_skill_sleight_of_hand
        Skill.STEALTH -> Res.string.sheet_skill_stealth
        Skill.SURVIVAL -> Res.string.sheet_skill_survival
    }

/** the second name under a skill that editions call differently, null for the skills with one name */
internal fun Skill.altRes(): StringResource? =
    when (this) {
        Skill.ANIMAL_HANDLING -> Res.string.sheet_skill_animal_handling_alt
        Skill.ARCANA -> Res.string.sheet_skill_arcana_alt
        Skill.INVESTIGATION -> Res.string.sheet_skill_investigation_alt
        Skill.PERCEPTION -> Res.string.sheet_skill_perception_alt
        Skill.PERFORMANCE -> Res.string.sheet_skill_performance_alt
        Skill.SLEIGHT_OF_HAND -> Res.string.sheet_skill_sleight_of_hand_alt
        else -> null
    }

internal fun Ability.nameRes(): StringResource =
    when (this) {
        Ability.STRENGTH -> Res.string.sheet_ability_strength
        Ability.DEXTERITY -> Res.string.sheet_ability_dexterity
        Ability.CONSTITUTION -> Res.string.sheet_ability_constitution
        Ability.INTELLIGENCE -> Res.string.sheet_ability_intelligence
        Ability.WISDOM -> Res.string.sheet_ability_wisdom
        Ability.CHARISMA -> Res.string.sheet_ability_charisma
    }

internal fun Ability.abbrRes(): StringResource =
    when (this) {
        Ability.STRENGTH -> Res.string.sheet_ability_strength_abbr
        Ability.DEXTERITY -> Res.string.sheet_ability_dexterity_abbr
        Ability.CONSTITUTION -> Res.string.sheet_ability_constitution_abbr
        Ability.INTELLIGENCE -> Res.string.sheet_ability_intelligence_abbr
        Ability.WISDOM -> Res.string.sheet_ability_wisdom_abbr
        Ability.CHARISMA -> Res.string.sheet_ability_charisma_abbr
    }

/** every name a skill is found by, lowercase, for the search of the sheet */
@Composable
internal fun rememberSkillSearchTexts(): Map<Skill, String> {
    val names = Skill.entries.associateWith { stringResource(it.nameRes()) }
    val alts = Skill.entries.mapNotNull { skill -> skill.altRes()?.let { skill to stringResource(it) } }.toMap()
    return remember(names, alts) {
        Skill.entries.associateWith { skill -> listOfNotNull(names[skill], alts[skill]).joinToString(" ").lowercase() }
    }
}
