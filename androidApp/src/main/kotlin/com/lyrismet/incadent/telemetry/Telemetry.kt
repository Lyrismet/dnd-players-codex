package com.lyrismet.incadent.telemetry

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lyrismet.incadent.core.analytics.AnalyticsTracker
import com.lyrismet.incadent.core.analytics.CrashReporter
import com.lyrismet.incadent.core.analytics.NoOpAnalyticsTracker
import com.lyrismet.incadent.core.analytics.NoOpCrashReporter

/** the analytics and crash reporter pair, both silent when the build has no Firebase config */
class Telemetry(
    val analytics: AnalyticsTracker,
    val crashReporter: CrashReporter,
) {
    companion object {
        fun create(context: Context): Telemetry =
            if (FirebaseApp.getApps(context).isEmpty()) {
                Telemetry(NoOpAnalyticsTracker, NoOpCrashReporter)
            } else {
                Telemetry(
                    FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context)),
                    FirebaseCrashReporter(FirebaseCrashlytics.getInstance()),
                )
            }
    }
}
