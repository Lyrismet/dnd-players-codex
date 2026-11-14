package com.lyrismet.incadent.feature.codex.domain.model

private val DiceParts = Regex("^(\\d{1,3})d(\\d{1,3})(.*)$", RegexOption.IGNORE_CASE)

private data class Dice(
    val count: Int,
    val sides: String,
    val tail: String,
) {
    fun text() = "${count}d$sides$tail"
}

private fun parseDice(text: String): Dice? =
    DiceParts.matchEntire(text.trim())?.destructured?.let { (count, sides, tail) -> Dice(count.toInt(), sides, tail) }

/** the dice [add] added [times] times to [base]: "1d8" + "1d8" twice is "3d8", "5" + "5" is "10" */
fun addDice(
    base: String,
    add: String,
    times: Int,
): String {
    val a = parseDice(base)
    val b = parseDice(add)
    val x = base.trim().toIntOrNull()
    val y = add.trim().toIntOrNull()
    return when {
        times <= 0 || add.isBlank() -> base
        a != null && b != null && a.sides == b.sides && b.tail.isEmpty() ->
            a
                .copy(
                    count = a.count + b.count * times,
                ).text()
        x != null && y != null -> (x + y * times).toString()
        else -> base + "+$add".repeat(times)
    }
}

/** the dice with their count multiplied, a cantrip of the fourth tier has 4d8 where the first has 1d8 */
fun multiplyDice(
    dice: String,
    factor: Int,
): String = parseDice(dice)?.let { it.copy(count = it.count * factor).text() } ?: dice
