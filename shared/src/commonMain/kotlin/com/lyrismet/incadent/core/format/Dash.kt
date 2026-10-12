package com.lyrismet.incadent.core.format

/** the placeholder every empty value shows, in lists, cards and sheets alike */
const val EMPTY_VALUE_DASH = "—"

/** the number, or the dash when it is not set */
fun Int?.orDash(): String = this?.toString() ?: EMPTY_VALUE_DASH

/** the text, or the dash when it is blank */
fun String?.orDash(): String = if (this.isNullOrBlank()) EMPTY_VALUE_DASH else this
