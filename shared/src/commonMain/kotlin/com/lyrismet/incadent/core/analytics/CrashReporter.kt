package com.lyrismet.incadent.core.analytics

/** reports handled failures, crashes are collected by the platform on their own */
interface CrashReporter {
    fun recordNonFatal(throwable: Throwable)
}

/** used where no crash backend is wired, like iOS for now or a build without the Firebase config */
object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable) = Unit
}
