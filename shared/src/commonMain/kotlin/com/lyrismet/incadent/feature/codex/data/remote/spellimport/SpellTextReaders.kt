package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.castKindOf
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedDuration
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedRange
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSchool

private val SchoolWords =
    mapOf(
        SpellSchool.ABJURATION to listOf("abjuration", "огражден"),
        SpellSchool.CONJURATION to listOf("conjuration", "вызов", "призыв"),
        SpellSchool.DIVINATION to listOf("divination", "прорицан"),
        SpellSchool.ENCHANTMENT to listOf("enchantment", "очарован"),
        SpellSchool.EVOCATION to listOf("evocation", "воплощен"),
        SpellSchool.ILLUSION to listOf("illusion", "иллюзи"),
        SpellSchool.NECROMANCY to listOf("necromancy", "некромант"),
        SpellSchool.TRANSMUTATION to listOf("transmutation", "преобразован"),
    )

/** the school named in a text in English or Russian, null for words that are none of the eight */
internal fun readSchool(text: String): SpellSchool? {
    val lower = text.lowercase()
    return SchoolWords.entries.firstOrNull { (_, words) -> words.any { it in lower } }?.key
}

private val FeetRange = Regex("^(\\d+)\\s*(feet|foot|ft|фут|фт)", RegexOption.IGNORE_CASE)

/** a range text read into feet, touch, self or the site's own words */
internal fun readRange(text: String): ImportedRange {
    val trimmed = text.trim()
    val lower = trimmed.lowercase()
    val feet =
        FeetRange
            .find(trimmed)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    return when {
        feet != null -> ImportedRange.Feet(feet)
        lower.startsWith("touch") || lower.startsWith("касани") -> ImportedRange.Touch
        lower.startsWith("self") || lower.startsWith("на себя") -> ImportedRange.Self
        else -> ImportedRange.Text(trimmed)
    }
}

private val UnitWords =
    mapOf(
        "round" to DurationUnit.ROUND,
        "раунд" to DurationUnit.ROUND,
        "minute" to DurationUnit.MINUTE,
        "минут" to DurationUnit.MINUTE,
        "hour" to DurationUnit.HOUR,
        "час" to DurationUnit.HOUR,
        "day" to DurationUnit.DAY,
        "дн" to DurationUnit.DAY,
        "ден" to DurationUnit.DAY,
        "сут" to DurationUnit.DAY,
    )
private val DurationNumber = Regex("(\\d+)\\s*(${UnitWords.keys.joinToString("|")})", RegexOption.IGNORE_CASE)

/** a duration read into a number of rounds, minutes, hours or days, [text] stays when the words fit none */
internal fun readDuration(text: String): ImportedDuration {
    val trimmed = text.trim()
    val lower = trimmed.lowercase()
    val match = DurationNumber.find(trimmed)
    return when {
        lower.startsWith("instant") || lower.startsWith("мгновен") -> ImportedDuration(DurationUnit.INSTANT, 0, "")
        match != null -> {
            val word = match.groupValues[2].lowercase()
            val unit = UnitWords.entries.first { word.startsWith(it.key) }.value
            ImportedDuration(unit, match.groupValues[1].toIntOrNull() ?: 0, "")
        }
        else -> ImportedDuration(DurationUnit.NONE, 0, trimmed)
    }
}

/** the kind of casting time and its own words when it is neither an action, a bonus action nor a reaction */
internal fun readCasting(text: String): Pair<CastKind, String> {
    val kind = castKindOf(text)
    return kind to if (kind == CastKind.LONG) text.trim() else ""
}

private val Dice = Regex("(\\d+)[dDкК](\\d+)")

/** the dice written in a text, like 8d6 or 2к10, in the order they appear and without repeats */
internal fun findDice(text: String): List<String> =
    Dice
        .findAll(text)
        .map { it.value }
        .distinct()
        .toList()
