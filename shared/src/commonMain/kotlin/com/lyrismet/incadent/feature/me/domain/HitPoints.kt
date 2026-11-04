package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals

/** how a number typed on the hit point keypad changes the hit points */
enum class HitPointMode { DAMAGE, HEAL, TEMP }

/** one change of the hit points: temporary ones soak up damage first, healing from zero ends the death saves */
fun CharacterVitals.hit(
    mode: HitPointMode,
    amount: Int,
): CharacterVitals {
    val value = amount.coerceAtLeast(0)
    val current = hpCurrent ?: 0
    return when (mode) {
        HitPointMode.DAMAGE -> {
            val soaked = minOf(tempHp, value)
            copy(hpCurrent = (current - (value - soaked)).coerceAtLeast(0), tempHp = tempHp - soaked)
        }
        HitPointMode.HEAL -> {
            val healed = hpMax?.let { minOf(it, current + value) } ?: (current + value)
            // a character back on their feet starts the death saves over
            copy(hpCurrent = healed).let {
                if (healed > 0) it.copy(deathSavesSuccess = 0, deathSavesFailure = 0) else it
            }
        }
        HitPointMode.TEMP -> copy(tempHp = value)
    }
}

/** where a character on zero hit points stands in the death saves */
enum class DeathSaveState { DYING, STABLE, DEAD }

private const val DEATH_SAVE_LIMIT = 3

fun CharacterVitals.deathSaveState(): DeathSaveState =
    when {
        deathSavesFailure >= DEATH_SAVE_LIMIT -> DeathSaveState.DEAD
        deathSavesSuccess >= DEATH_SAVE_LIMIT -> DeathSaveState.STABLE
        else -> DeathSaveState.DYING
    }

/** a character is down when they have hit points to lose and none left */
val CharacterVitals.isDown: Boolean get() = hpMax != null && hpCurrent == 0

/** a death save dot pressed: the count moves to that dot, pressing the last filled one empties it */
fun Int.withDeathSaveDot(index: Int): Int = if (index < this) index else index + 1
