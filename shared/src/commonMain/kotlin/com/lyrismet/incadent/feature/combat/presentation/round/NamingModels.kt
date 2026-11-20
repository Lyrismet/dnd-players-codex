package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.round.NameChoice

/** the name field of a participant made of a species, what is typed in it and what it can turn into */
data class NamingState(
    val value: String,
    val placeholder: String,
    val options: List<NameOption>,
    // the class and subclass, set under the options before the name is given
    val role: RoleField,
    // what goes to the card of a new npc with the name, shown under the options
    val carry: List<String>,
    // the role that goes to the card, shown first and lit
    val carryRole: String,
    // the cursor goes into the field when the name in the header was pressed
    val focus: Boolean,
)

data class NameOption(
    val choice: NameChoice,
    val title: String,
    // empty for the caption-only option
    val subtitle: String,
)

/** the sheet that follows the naming of a participant and tells what the new link means */
data class PromotionSheet(
    val overline: String,
    val name: String,
    val rows: List<PromotionRow>,
    val confirmLabel: String,
)

data class PromotionRow(
    val title: String,
    val value: String,
    val hint: String,
)
