package com.lyrismet.incadent.core.analytics

/** reports usage events, the platform decides where they go */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/** used where no analytics backend is wired, like iOS for now or a build without the Firebase config */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
