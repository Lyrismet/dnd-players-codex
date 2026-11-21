package com.lyrismet.incadent.core.analytics

import androidx.compose.runtime.staticCompositionLocalOf

/** the tracker for code that is built inside a composition, a presenter that is injected takes it as a parameter */
val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> { NoOpAnalyticsTracker }
