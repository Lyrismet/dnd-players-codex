package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect

/** which round the summary shows, which blow's term has its answer open and whether the answer goes to the bestiary */
internal data class SummaryDraft(
    val round: Int? = null,
    val openEventId: Long? = null,
    val record: Boolean = true,
)

/** what the summary may use at the moment of an event */
internal class SummarySnapshot(
    val speciesOf: Map<Long, Species>,
    val typeNames: Map<DamageType, String>,
    val effectWords: Map<HitEffect, String>,
)

internal fun HitEffect.toDamageState(): DamageState =
    when (this) {
        HitEffect.FULL -> DamageState.NORMAL
        HitEffect.HALF -> DamageState.RESISTANCE
        HitEffect.NONE -> DamageState.IMMUNITY
        HitEffect.DOUBLE -> DamageState.VULNERABILITY
    }
