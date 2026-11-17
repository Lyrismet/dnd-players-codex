package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.math.abs

private const val FULL = 1000L

/** the words of the scale from "down" at the left end to "whole" at the right, each with its place in permille */
val HitScaleWords: List<Pair<CombatState, Int>> =
    listOf(
        CombatState.DOWN to 0,
        CombatState.BARELY_STANDING to 250,
        CombatState.BLOODIED to 500,
        CombatState.WOUNDED to 750,
        CombatState.WHOLE to FULL.toInt(),
    )

/** the word nearest to a place on the scale, a tie goes to the healthier one */
fun stateAtPermille(permille: Int): CombatState = HitScaleWords.asReversed().minBy { abs(it.second - permille) }.first

/** where a word sits on the scale, none for a state that is not on it */
fun permilleOf(state: CombatState): Int? = HitScaleWords.firstOrNull { it.first == state }?.second

/** the place a typed number takes on the scale, what is left of all the hit points that were there */
fun permilleOfExact(
    remaining: Int,
    netDamage: Int,
): Int = if (remaining + netDamage <= 0) 0 else (remaining * FULL / (remaining + netDamage)).toInt()
