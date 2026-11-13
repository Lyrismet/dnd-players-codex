package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType

/** what the sheet of the features has typed for one participant, [editing] is the fact the field was opened from */
internal data class FeaturesDraft(
    val memberId: Long,
    val text: String = "",
    val editing: String? = null,
)

data class ShieldItem(
    val type: DamageType,
    val state: DamageState,
)

/** the block at the top of the marks tab: the shields in one line, the facts in one more, the rest behind "ещё" */
data class FeaturesBlock(
    val shields: List<ShieldItem>,
    val facts: List<String>,
    // no record of the codex holds them, they live in the fight only
    val onlyInFight: Boolean,
    val hidden: Int,
    // every damage type with its state, the tiles of the shield editing in the block
    val cells: List<ShieldCell>,
)

data class ShieldCell(
    val type: DamageType,
    val state: DamageState?,
)

data class FeaturesSheet(
    val name: String,
    val onlyInFight: Boolean,
    val facts: List<String>,
    val text: String,
    val canAdd: Boolean,
    val shields: Map<DamageType, DamageState>,
)

internal const val FEATURE_SHIELDS_SHOWN = 5
internal const val FEATURE_FACTS_SHOWN = 2
