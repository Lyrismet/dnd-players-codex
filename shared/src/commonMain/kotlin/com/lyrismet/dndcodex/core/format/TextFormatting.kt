package com.lyrismet.dndcodex.core.format

/** capitalizes just the first character - a name typed all-lowercase still reads as a proper noun */
fun String.capitalizeFirst(): String = if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
