package com.lyrismet.incadent.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lyrismet.incadent.core.analytics.CrashReporter

/** sends handled failures to Crashlytics */
class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {
    override fun recordNonFatal(throwable: Throwable) = crashlytics.recordException(throwable)
}
