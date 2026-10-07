package com.lyrismet.incadent.core.format

private const val HUNDRED = 100
private const val TEN = 10
private const val FEW_RANGE_START = 2
private const val FEW_RANGE_END = 4
private const val TEEN_EXCEPTIONS_START = 12
private const val TEEN_EXCEPTIONS_END = 14
private const val ONE_EXCEPTION = 11

/**
 * the mockup's `plural()` - the standard Russian plural-form rule for noun agreement with a count:
 * 1 (but not 11) picks [one], 2-4 (but not 12-14) picks [few], everything else picks [many].
 * Reused wherever a Russian count needs a matching noun - notes, rounds, targets, mentions and so on.
 */
fun russianPluralForm(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val mod100 = count % HUNDRED
    val mod10 = count % TEN
    return when {
        mod10 == 1 && mod100 != ONE_EXCEPTION -> one
        mod10 in FEW_RANGE_START..FEW_RANGE_END && mod100 !in TEEN_EXCEPTIONS_START..TEEN_EXCEPTIONS_END -> few
        else -> many
    }
}
