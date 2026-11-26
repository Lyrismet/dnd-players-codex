package com.lyrismet.incadent.core.errors

import com.lyrismet.incadent.core.analytics.CrashReporter
import com.lyrismet.incadent.core.analytics.NoOpCrashReporter
import com.lyrismet.incadent.core.logging.AppLog

/** a handled failure as the crash backend sees it, the component and the message make its group readable */
class ReportedError(
    component: String,
    message: String,
    cause: Throwable,
) : Exception("[$component] $message", cause)

/** the one place a handled failure goes to: it is logged and sent to the crash backend as a non-fatal */
object AppErrors {
    private var reporter: CrashReporter = NoOpCrashReporter

    /** called once when the graph is built, before that failures are only logged */
    fun install(reporter: CrashReporter) {
        this.reporter = reporter
    }

    /** a failure worth seeing in the backend, expected ones like no connection are only logged with [AppLog] */
    fun report(
        component: String,
        message: String,
        failure: Throwable,
    ) {
        AppLog.error(component, message, failure)
        // reporting is never allowed to fail the action that is already handling a failure
        runCatching { reporter.recordNonFatal(ReportedError(component, message, failure)) }
    }
}
