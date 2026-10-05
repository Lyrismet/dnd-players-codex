package com.lyrismet.incadent.core.codexgroup

/** the value itself, or null for a blank or dash placeholder the forms store for "not set" */
internal fun String?.valueOrNull(): String? = this?.takeIf { it.isNotBlank() && it != "—" }

/** approximates russian alphabetical order without an ICU collator - case-insensitive, with ё sorted as е */
internal fun String.collationKey(): String = lowercase().replace('ё', 'е')
