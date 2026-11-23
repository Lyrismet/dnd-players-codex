package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

internal fun parseObject(body: String): JsonObject? =
    runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()

private fun JsonElement?.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.text(key: String): String? = this[key].string()

internal fun JsonObject.number(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.flag(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

/** a field that is one text or a list of paragraphs, joined into one text with a blank line between */
internal fun JsonObject.paragraphs(key: String): String =
    when (val value = this[key]) {
        is JsonArray -> value.mapNotNull { it.string() }.joinToString("\n\n")
        else -> value.string().orEmpty()
    }

/** the description with the text about higher levels after it, the growth itself is not read from it */
internal fun JsonObject.descriptionWithHigherLevel(): String =
    listOf(paragraphs("desc"), paragraphs("higher_level")).filter { it.isNotBlank() }.joinToString("\n\n")

/** a field like "yes" or "no", or a flag */
internal fun JsonObject.yes(key: String): Boolean = flag(key) ?: text(key)?.equals("yes", ignoreCase = true) ?: false
