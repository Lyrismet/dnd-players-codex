package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

private val DigitBeforeLetter = Regex("(\\d)([A-Za-z])")

/** a spell of dnd5eapi.co, null when the answer is not one */
internal fun readDnd5eApiSpell(body: String): ImportedSpell? {
    val json = parseObject(body) ?: return null
    val level = json.number("level") ?: -1
    val components =
        (json["components"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.uppercase() }
            .orEmpty()
    return SpellFields(
        source = SpellSource.DND5E_API,
        name = json.text("name").orEmpty(),
        level = level,
        schoolText = json.obj("school")?.text("name").orEmpty(),
        castingText = json.text("casting_time").orEmpty(),
        rangeText = json.text("range").orEmpty(),
        verbal = "V" in components,
        somatic = "S" in components,
        material = if ("M" in components) json.text("material").orEmpty() else null,
        durationText = json.text("duration").orEmpty(),
        concentration = json.yes("concentration"),
        ritual = json.yes("ritual"),
        description = json.descriptionWithHigherLevel(),
        damage = readDamage(level, json.apiDamageTables()),
    ).toImportedSpell()
}

/** a spell of the v1 api of Open5e, null when the answer is not one */
internal fun readOpen5eV1Spell(body: String): ImportedSpell? {
    val json = parseObject(body) ?: return null
    return SpellFields(
        source = SpellSource.OPEN5E,
        name = json.text("name").orEmpty(),
        level = json.number("level_int") ?: -1,
        schoolText = json.text("school").orEmpty(),
        castingText = json.text("casting_time").orEmpty(),
        rangeText = json.text("range").orEmpty(),
        verbal = json.yes("requires_verbal_components"),
        somatic = json.yes("requires_somatic_components"),
        material = if (json.yes("requires_material_components")) json.text("material").orEmpty() else null,
        durationText = json.text("duration").orEmpty(),
        concentration = json.yes("concentration"),
        ritual = json.yes("ritual"),
        description = json.descriptionWithHigherLevel(),
    ).toImportedSpell()
}

/** a spell of the v2 api of Open5e, null when the answer is not one */
internal fun readOpen5eV2Spell(body: String): ImportedSpell? {
    val json = parseObject(body) ?: return null
    val level = json.number("level") ?: -1
    return SpellFields(
        source = SpellSource.OPEN5E,
        name = json.text("name").orEmpty(),
        level = level,
        schoolText = json.obj("school")?.text("name").orEmpty(),
        // "bonus_action" and "1minute" are the api's keys, they read like the words of a site once spaced
        castingText =
            json
                .text("casting_time")
                .orEmpty()
                .replace('_', ' ')
                .replace(DigitBeforeLetter, "$1 $2"),
        rangeText = json.text("range_text").orEmpty(),
        verbal = json.yes("verbal"),
        somatic = json.yes("somatic"),
        material = if (json.yes("material")) json.text("material_specified").orEmpty() else null,
        durationText = json.text("duration").orEmpty(),
        concentration = json.yes("concentration"),
        ritual = json.yes("ritual"),
        description = json.descriptionWithHigherLevel(),
        damage = readDamage(level, json.open5eDamageTables()),
    ).toImportedSpell()
}
