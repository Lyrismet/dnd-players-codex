package com.lyrismet.incadent.feature.codex.domain.model

private const val ROW_SEPARATOR = ';'
private const val KEY_SEPARATOR = ':'
private const val HEALING_KEY = "HEAL"
private const val TEMP_KEY = "TEMP"
private val DiceFormat = Regex("^(\\d{1,3}d\\d{1,3}|\\d{1,4})([+-](\\d{1,3}|мод|mod))*$", RegexOption.IGNORE_CASE)

/** a dice text as the form accepts it: "1d10", "2d6+3", "5", "1d8+мод", an empty one is fine */
fun isValidDice(dice: String): Boolean = dice.isBlank() || DiceFormat.matches(dice.trim())

private fun SpellEffect?.key(): String =
    when (this) {
        null -> ""
        SpellEffect.Healing -> HEALING_KEY
        SpellEffect.TempHp -> TEMP_KEY
        is SpellEffect.Damage -> type.name
    }

private fun effectOf(key: String): SpellEffect? =
    when (key) {
        HEALING_KEY -> SpellEffect.Healing
        TEMP_KEY -> SpellEffect.TempHp
        else -> DamageType.entries.firstOrNull { it.name == key }?.let { SpellEffect.Damage(it) }
    }

/** the rows as one column text, "COLD:2d6;PIERCING:1d10", separators are dropped from the dice */
fun encodeDamage(rows: List<SpellDamage>): String =
    rows.joinToString(ROW_SEPARATOR.toString()) { row ->
        val dice = row.dice.filter { it != ROW_SEPARATOR && it != KEY_SEPARATOR && !it.isWhitespace() }
        "${row.effect.key()}$KEY_SEPARATOR$dice"
    }

fun decodeDamage(text: String): List<SpellDamage> =
    if (text.isBlank()) {
        emptyList()
    } else {
        text.split(ROW_SEPARATOR).mapNotNull { part ->
            val at = part.indexOf(KEY_SEPARATOR)
            if (at < 0) null else SpellDamage(part.substring(at + 1), effectOf(part.substring(0, at)))
        }
    }
