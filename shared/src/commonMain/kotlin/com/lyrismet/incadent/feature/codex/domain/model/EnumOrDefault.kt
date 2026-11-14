package com.lyrismet.incadent.feature.codex.domain.model

/** the enum entry of a stored name, the default for a name this version does not know */
inline fun <reified T : Enum<T>> enumOrDefault(
    name: String?,
    default: T,
): T = enumValues<T>().firstOrNull { it.name == name } ?: default
