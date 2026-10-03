package com.lyrismet.incadent.core.format

/** capitalizes just the first character - a name typed all-lowercase still reads as a proper noun */
fun String.capitalizeFirst(): String = if (isEmpty()) this else this[0].uppercaseChar() + substring(1)

/** joins the non-blank parts with " · " so an empty field never leaves a dangling separator */
fun joinWithDot(vararg parts: String): String = parts.filter { it.isNotBlank() }.joinToString(" · ")
