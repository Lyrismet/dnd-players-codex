package com.lyrismet.incadent.core.logging

import co.touchlab.kermit.Logger

private const val APP_TAG = "DnDCodex"

/** the app's one logging entry point - every line reads "[component]: message" */
object AppLog {
    fun info(
        component: String,
        message: String,
    ) = Logger.withTag(APP_TAG).i { "[$component]: $message" }

    fun warn(
        component: String,
        message: String,
    ) = Logger.withTag(APP_TAG).w { "[$component]: $message" }

    fun error(
        component: String,
        message: String,
        throwable: Throwable? = null,
    ) = Logger.withTag(APP_TAG).e(throwable) { "[$component]: $message" }
}
