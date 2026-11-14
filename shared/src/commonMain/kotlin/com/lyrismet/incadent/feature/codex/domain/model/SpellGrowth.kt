package com.lyrismet.incadent.feature.codex.domain.model

const val MAX_GROWTH_RULES = 6
const val MAX_GROWTH_STEPS = 9
const val MAX_GROWTH_TEXT = 60
val GROWTH_EVERY_RANGE = 2..9

/** the character levels at which a cantrip gets stronger */
val CANTRIP_TIERS = listOf(5, 11, 17)

/** what a cantrip gets more of with the character level: dice of its damage rows or the projectiles */
enum class CantripGrowth { NONE, DICE, PROJECTILES }

/** what a rule makes bigger on a higher slot */
enum class GrowthTarget { DICE, PROJECTILES, DURATION, TARGETS, RANGE, CUSTOM }

/** how a rule grows: by the same amount each circle, each N circles, or by a table of circles and values */
enum class GrowthMode { EVERY, EVERY_N, STEPS }

/** a value that starts at the circle [from], a duration step reads "HOUR:8", a dice step "2d8" */
data class GrowthStep(
    val from: Int,
    val value: String,
)

/** one thing that grows, [damageIndex] names the damage row of a dice rule */
data class GrowthRule(
    val target: GrowthTarget,
    val mode: GrowthMode = GrowthMode.EVERY,
    val damageIndex: Int = 0,
    val every: Int = 2,
    val add: String = "",
    val steps: List<GrowthStep> = emptyList(),
) {
    /** a range and a free note have no amount to add, only a table */
    val stepsOnly: Boolean get() = target == GrowthTarget.RANGE || target == GrowthTarget.CUSTOM

    fun clamped(): GrowthRule =
        copy(
            mode = if (stepsOnly) GrowthMode.STEPS else mode,
            damageIndex = damageIndex.coerceAtLeast(0),
            every = every.coerceIn(GROWTH_EVERY_RANGE),
            add = add.take(MAX_GROWTH_TEXT),
            steps =
                steps
                    .map { GrowthStep(it.from.coerceIn(SPELL_LEVELS), it.value.take(MAX_GROWTH_TEXT)) }
                    .sortedBy { it.from }
                    .take(MAX_GROWTH_STEPS),
        )
}

/** how a spell gets stronger: [cantrip] by character level, or the [rules] by slot level while [enabled] */
data class SpellGrowth(
    val enabled: Boolean = false,
    val cantrip: CantripGrowth = CantripGrowth.NONE,
    val rules: List<GrowthRule> = emptyList(),
) {
    fun clamped(): SpellGrowth = copy(rules = rules.take(MAX_GROWTH_RULES).map { it.clamped() })
}

/** the "HOUR:8" text of a duration step */
fun durationStep(
    unit: DurationUnit,
    value: Int,
): String = "${unit.name}:$value"

fun parseDurationStep(text: String): Pair<DurationUnit, Int>? {
    val unit = DurationUnit.entries.firstOrNull { it.name == text.substringBefore(':') } ?: return null
    return text.substringAfter(':', "").toIntOrNull()?.let { unit to it }
}
