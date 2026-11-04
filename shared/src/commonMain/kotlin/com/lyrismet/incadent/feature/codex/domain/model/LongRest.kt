package com.lyrismet.incadent.feature.codex.domain.model

/** what the player reads before a long rest: the hit points and how much else comes back */
data class RestSummary(
    val hpBefore: Int?,
    val hpAfter: Int?,
    val hpFull: Boolean,
    val hasSpellSlots: Boolean,
    val resourceCount: Int,
)

/** the values a rest changed as they were, putting them back leaves every other edit alone */
data class RestUndo(
    val vitals: CharacterVitals,
    val resourceCounts: Map<Long, Int>,
)

data class RestPlan(
    val vitals: CharacterVitals,
    val resources: List<CharacterResource>,
    val summary: RestSummary,
    val undo: RestUndo,
)

/** a long rest returns everything: all hit points, no temporary ones, all slots and resources, no death saves */
object LongRest {
    fun plan(
        vitals: CharacterVitals,
        resources: List<CharacterResource>,
    ): RestPlan {
        val hpAfter = vitals.hpMax ?: vitals.hpCurrent
        val rested = resources.map { it.copy(current = it.max) }
        val changed = resources.zip(rested).filter { (before, after) -> before.current != after.current }
        return RestPlan(
            vitals =
                vitals.copy(
                    hpCurrent = hpAfter,
                    tempHp = 0,
                    deathSavesSuccess = 0,
                    deathSavesFailure = 0,
                ),
            resources = rested,
            summary =
                RestSummary(
                    hpBefore = vitals.hpCurrent,
                    hpAfter = hpAfter,
                    hpFull = vitals.hpMax != null && vitals.hpCurrent == vitals.hpMax,
                    hasSpellSlots = resources.any { it.isSpellSlot },
                    resourceCount = resources.count { !it.isSpellSlot },
                ),
            undo = RestUndo(vitals, changed.associate { (before, _) -> before.id to before.current }),
        )
    }
}
