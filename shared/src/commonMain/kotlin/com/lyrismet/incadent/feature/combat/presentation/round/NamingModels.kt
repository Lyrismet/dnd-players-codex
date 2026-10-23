package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice

/** the name field of a participant made of a species, what is typed in it and what it can turn into */
data class NamingState(
    val value: String,
    val placeholder: String,
    val options: List<NameOption>,
    // what goes to the card of a new npc with the name, shown under the options
    val carry: List<String>,
)

data class NameOption(
    val choice: NameChoice,
    val title: String,
    // empty for the caption-only option
    val subtitle: String,
)

/** the sheet that follows the naming of a participant: what the new link means, and what the fight learned */
data class PromotionSheet(
    val overline: String,
    val name: String,
    val rows: List<PromotionRow>,
    val learned: List<LearnedRow>,
    val confirmLabel: String,
)

data class PromotionRow(
    val title: String,
    val value: String,
    val hint: String,
)

data class LearnedRow(
    val type: DamageType,
    val state: DamageState,
    val label: String,
    // true when it goes to the npc only, false when the whole species keeps it
    val onlyNpc: Boolean,
    val allLabel: String,
    val onlyLabel: String,
)
