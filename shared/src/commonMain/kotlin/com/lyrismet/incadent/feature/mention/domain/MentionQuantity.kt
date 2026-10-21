package com.lyrismet.incadent.feature.mention.domain

// the species token a draft ends with, its count not typed yet, then the single space the picker leaves
private val PENDING_REGEX = Regex("@\\[([^\\[\\]|]*)\\|s:\\d+] $")

// the same token once a count is in it, the caret still right behind it
private val COUNTED_REGEX = Regex("(@\\[[^\\[\\]|]*\\|s:\\d+\\|)(\\d+)] $")

/** the draft after a digit became part of a species token, with the caret right behind that token */
data class QuantityEdit(
    val text: String,
    val caret: Int,
)

/** the species name a draft has just inserted and still waits a count for, null once anything else was typed */
fun pendingQuantityName(
    draft: String,
    caret: Int,
): String? =
    PENDING_REGEX
        .find(draft)
        ?.takeIf { caret == draft.length }
        ?.groupValues
        ?.get(1)
        ?.replace('_', ' ')

/**
 * turns the digit typed behind a species token into its count. The first digit follows the space the picker left,
 * the next ones follow the token, so "12" reads as twelve. Null when the change is not such a digit.
 */
fun typedQuantityEdit(
    previous: String,
    previousCaret: Int,
    next: String,
): QuantityEdit? = startedQuantity(previous, previousCaret, next) ?: extendedQuantity(previous, previousCaret, next)

private fun startedQuantity(
    previous: String,
    previousCaret: Int,
    next: String,
): QuantityEdit? {
    val atEnd = previousCaret == previous.length && PENDING_REGEX.containsMatchIn(previous)
    val typed = next.takeIf { atEnd && it.length == previous.length + 1 && it.startsWith(previous) }
    val digit = typed?.last()?.takeIf { it in '1'..'9' }
    return digit?.let { caretBeforeSpace(previous.dropLast(2) + "|$it] ") }
}

private fun extendedQuantity(
    previous: String,
    previousCaret: Int,
    next: String,
): QuantityEdit? {
    val counted = COUNTED_REGEX.find(previous)?.takeIf { previousCaret == previous.length - 1 }
    val typed = next.length == previous.length + 1 && next.startsWith(previous.dropLast(1)) && next.endsWith(" ")
    val digit = next.getOrNull(previous.length - 1)?.takeIf { typed && it in '0'..'9' }
    if (counted == null || digit == null) return null
    val grown = (counted.groupValues[2] + digit).toIntOrNull()?.takeIf { it <= MAX_MENTION_QUANTITY }
    // a count past the cap swallows the digit, the draft stays as it was
    return if (grown == null) {
        QuantityEdit(previous, previousCaret)
    } else {
        caretBeforeSpace(previous.dropLast(counted.groupValues[2].length + 2) + grown + "] ")
    }
}

private fun caretBeforeSpace(text: String) = QuantityEdit(text, text.length - 1)

/** the counts of the tokens that point at [ref] in [body], in the order they are written */
fun tokenQuantities(
    body: String,
    ref: MentionRef,
): List<Int> =
    TOKEN_REGEX
        .findAll(body)
        .filter { it.toRef() == ref }
        .map { it.quantity() }
        .toList()

/** [body] with the count of the [occurrence]th token of [ref] set to [quantity], clamped to 1 until the cap */
fun withTokenQuantity(
    body: String,
    ref: MentionRef,
    occurrence: Int,
    quantity: Int,
): String {
    var seen = -1
    return TOKEN_REGEX.replace(body) { token ->
        if (token.toRef() == ref && ++seen == occurrence) {
            mentionToken(ref, token.groupValues[NAME_GROUP], quantity.coerceIn(1, MAX_MENTION_QUANTITY))
        } else {
            token.value
        }
    }
}
