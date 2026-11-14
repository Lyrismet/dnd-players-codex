package com.lyrismet.incadent.feature.codex.domain.model

private const val RULE_SEPARATOR = ';'
private const val FIELD_SEPARATOR = ','
private const val STEP_SEPARATOR = '|'
private const val PAIR_SEPARATOR = ':'
private const val PART_SEPARATOR = '~'
private const val ESCAPE = '%'
private const val HEX = 16
private const val ESCAPE_DIGITS = 2
private const val RULE_FIELDS = 6
private const val PART_COUNT = 2
private val Special = listOf(ESCAPE, RULE_SEPARATOR, FIELD_SEPARATOR, STEP_SEPARATOR, PAIR_SEPARATOR, PART_SEPARATOR)

private fun escape(text: String): String =
    buildString {
        text.forEach { c ->
            if (c in Special) {
                append(ESCAPE)
                append(c.code.toString(HEX).padStart(ESCAPE_DIGITS, '0'))
            } else {
                append(c)
            }
        }
    }

private fun unescape(text: String): String =
    buildString {
        var i = 0
        while (i < text.length) {
            val code =
                if (text[i] ==
                    ESCAPE
                ) {
                    text.substring(i + 1, minOf(i + 1 + ESCAPE_DIGITS, text.length)).toIntOrNull(HEX)
                } else {
                    null
                }
            if (code != null) {
                append(code.toChar())
                i += 1 + ESCAPE_DIGITS
            } else {
                append(text[i])
                i++
            }
        }
    }

/** the growth as one column text: "1~NONE~DICE,EVERY,0,2,1d6,3:2d8|5:3d8;...", free text is percent-escaped */
fun encodeGrowth(growth: SpellGrowth): String {
    if (growth == SpellGrowth()) return ""
    val rules =
        growth.rules.joinToString(RULE_SEPARATOR.toString()) { rule ->
            val steps =
                rule.steps.joinToString(
                    STEP_SEPARATOR.toString(),
                ) { "${it.from}$PAIR_SEPARATOR${escape(it.value)}" }
            listOf(rule.target.name, rule.mode.name, rule.damageIndex, rule.every, escape(rule.add), steps)
                .joinToString(FIELD_SEPARATOR.toString())
        }
    return listOf(if (growth.enabled) "1" else "0", growth.cantrip.name, rules).joinToString(PART_SEPARATOR.toString())
}

fun decodeGrowth(text: String): SpellGrowth {
    val parts = text.split(PART_SEPARATOR)
    if (text.isBlank() || parts.size < PART_COUNT) return SpellGrowth()
    return SpellGrowth(
        enabled = parts[0] == "1",
        cantrip = enumOrDefault(parts[1], CantripGrowth.NONE),
        rules =
            parts
                .getOrNull(PART_COUNT)
                ?.split(RULE_SEPARATOR)
                ?.mapNotNull { decodeRule(it) }
                .orEmpty(),
    )
}

private fun decodeSteps(text: String): List<GrowthStep> =
    text.split(STEP_SEPARATOR).mapNotNull { pair ->
        val at = pair.indexOf(PAIR_SEPARATOR)
        val from = if (at < 0) null else pair.substring(0, at).toIntOrNull()
        if (from == null) null else GrowthStep(from, unescape(pair.substring(at + 1)))
    }

private fun decodeRule(text: String): GrowthRule? {
    val f = text.split(FIELD_SEPARATOR)
    val target = GrowthTarget.entries.firstOrNull { it.name == f.firstOrNull() }
    return if (f.size < RULE_FIELDS || target == null) {
        null
    } else {
        val next = f.drop(1).iterator()
        GrowthRule(
            target = target,
            mode = enumOrDefault(next.next(), GrowthMode.EVERY),
            damageIndex = next.next().toIntOrNull() ?: 0,
            every = next.next().toIntOrNull() ?: GROWTH_EVERY_RANGE.first,
            add = unescape(next.next()),
            steps = decodeSteps(next.next()),
        ).clamped()
    }
}
