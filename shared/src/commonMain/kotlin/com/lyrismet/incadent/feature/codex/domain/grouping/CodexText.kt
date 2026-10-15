package com.lyrismet.incadent.feature.codex.domain.grouping

/** the value itself, or null for a blank or dash placeholder the forms store for "not set" */
internal fun String?.valueOrNull(): String? = this?.takeIf { it.isNotBlank() && it != "—" }
