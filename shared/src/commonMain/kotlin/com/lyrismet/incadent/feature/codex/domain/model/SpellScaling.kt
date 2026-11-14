package com.lyrismet.incadent.feature.codex.domain.model

private fun CharacterSpell.base() = SpellAt(damage, projectiles, durationUnit, durationValue, 0, range, emptyList())

/** how many of the cantrip's tiers the character level has reached, 0 to 3 */
fun cantripTier(characterLevel: Int): Int = CANTRIP_TIERS.count { characterLevel >= it }

/** a cantrip with its growth by the level of the character, what a spell with no growth has is its own fields */
fun CharacterSpell.atCharacterLevel(characterLevel: Int): SpellAt {
    val tier = cantripTier(characterLevel)
    val now = base()
    return when (growth.cantrip) {
        CantripGrowth.NONE -> now
        CantripGrowth.DICE -> now.copy(damage = now.damage.map { it.copy(dice = multiplyDice(it.dice, tier + 1)) })
        CantripGrowth.PROJECTILES -> now.copy(projectiles = now.projectiles + tier)
    }
}

/** a cantrip by the character level, any other spell by the slot it is cast with */
fun CharacterSpell.atRank(
    characterLevel: Int,
    circle: Int = level,
): SpellAt = if (isCantrip) atCharacterLevel(characterLevel) else atCircle(circle)

/** a spell cast with the slot of [circle], its own circle gives the base fields */
fun CharacterSpell.atCircle(circle: Int): SpellAt {
    val now = base()
    if (!growth.enabled || isCantrip || circle <= level) return now
    return growth.rules.fold(now) { at, rule -> rule.apply(at, circle - level, circle) }
}

private fun GrowthRule.times(
    over: Int,
    circle: Int,
): Int =
    when (mode) {
        GrowthMode.EVERY -> over
        GrowthMode.EVERY_N -> over / every
        GrowthMode.STEPS -> if (steps.any { it.from <= circle }) 1 else 0
    }

private fun GrowthRule.stepValue(circle: Int): String? = steps.lastOrNull { it.from <= circle }?.value

private fun GrowthRule.apply(
    at: SpellAt,
    over: Int,
    circle: Int,
): SpellAt {
    val times = times(over, circle)
    val step = stepValue(circle)
    val amount = add.trim().toIntOrNull() ?: 0
    return when (target) {
        GrowthTarget.DICE -> at.copy(damage = diceAt(at.damage, times, step))
        GrowthTarget.PROJECTILES -> at.copy(projectiles = numberAt(at.projectiles, amount * times, step))
        GrowthTarget.DURATION -> durationAt(at, times, amount, step)
        GrowthTarget.TARGETS -> at.copy(extraTargets = numberAt(at.extraTargets, amount * times, step))
        GrowthTarget.RANGE -> if (step == null) at else at.copy(range = step)
        GrowthTarget.CUSTOM -> if (step == null) at else at.copy(notes = at.notes + step)
    }
}

private fun GrowthRule.diceAt(
    rows: List<SpellDamage>,
    times: Int,
    step: String?,
): List<SpellDamage> =
    rows.mapIndexed { i, row ->
        when {
            i != damageIndex -> row
            mode != GrowthMode.STEPS -> row.copy(dice = addDice(row.dice, add, times))
            step == null -> row
            else -> row.copy(dice = step)
        }
    }

// a step is the whole number, the other modes add to what the spell has
private fun GrowthRule.numberAt(
    now: Int,
    added: Int,
    step: String?,
): Int = if (mode == GrowthMode.STEPS) step?.toIntOrNull() ?: now else now + added

private fun GrowthRule.durationAt(
    at: SpellAt,
    times: Int,
    amount: Int,
    step: String?,
): SpellAt =
    if (mode == GrowthMode.STEPS) {
        step?.let(::parseDurationStep)?.let { (unit, value) -> at.copy(durationUnit = unit, durationValue = value) }
            ?: at
    } else {
        at.copy(durationValue = (at.durationValue + amount * times).coerceAtMost(MAX_DURATION_VALUE))
    }
