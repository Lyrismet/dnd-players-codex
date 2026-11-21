package com.lyrismet.incadent.telemetry

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.lyrismet.incadent.core.analytics.AnalyticsEvent
import com.lyrismet.incadent.core.analytics.AnalyticsTracker

/** sends the app's events to Firebase Analytics */
class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        val bundle = Bundle().apply { event.params.forEach { (key, value) -> putString(key, value) } }
        analytics.logEvent(event.name, bundle)
    }
}
