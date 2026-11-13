package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.sheet_ability_charisma_abbr
import incadent.shared.generated.resources.sheet_ability_constitution_abbr
import incadent.shared.generated.resources.sheet_ability_dexterity_abbr
import incadent.shared.generated.resources.sheet_ability_intelligence_abbr
import incadent.shared.generated.resources.sheet_ability_strength_abbr
import incadent.shared.generated.resources.sheet_ability_wisdom_abbr
import org.jetbrains.compose.resources.StringResource

internal fun Ability.abbrRes(): StringResource =
    when (this) {
        Ability.STRENGTH -> Res.string.sheet_ability_strength_abbr
        Ability.DEXTERITY -> Res.string.sheet_ability_dexterity_abbr
        Ability.CONSTITUTION -> Res.string.sheet_ability_constitution_abbr
        Ability.INTELLIGENCE -> Res.string.sheet_ability_intelligence_abbr
        Ability.WISDOM -> Res.string.sheet_ability_wisdom_abbr
        Ability.CHARISMA -> Res.string.sheet_ability_charisma_abbr
    }
